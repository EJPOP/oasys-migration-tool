package oasys.migration.alias;

import java.util.*;

public class FromJoinAliasResolver {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "WHERE", "GROUP", "ORDER", "HAVING", "UNION", "INTERSECT", "EXCEPT", "MINUS",
            "CONNECT", "START", "MODEL", "FETCH", "FOR", "RETURNING",
            "WHEN", "THEN", "ELSE", "END", "ON"
    ));

    private static final Set<String> JOIN_WORDS = new HashSet<>(Arrays.asList(
            "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "CROSS"
    ));

    private static final Set<String> DML_WORDS = new HashSet<>(Arrays.asList(
            "UPDATE", "DELETE", "INSERT", "MERGE"
    ));

    public static Map<String, String> resolve(String sql) {
        Map<String, String> aliasToTable = new LinkedHashMap<>();
        if (sql == null || sql.isEmpty()) return aliasToTable;

        Scan st = new Scan(sql);

        while (st.hasNext()) {
            if (st.peekIsLineComment()) { st.readLineComment(); continue; }
            if (st.peekIsBlockComment()) { st.readBlockComment(); continue; }
            if (st.peekIsSingleQuotedString()) { st.readSingleQuotedString(); continue; }

            if (st.peekWord("FROM")) {
                st.readWord(); // FROM
                st.readSpaces();
                parseFromSources(st, aliasToTable);
                continue;
            }

            if (st.peekWord("JOIN")) {
                st.readWord();
                st.readSpaces();
                parseOneSource(st, aliasToTable);
                continue;
            }

            if (st.peekWord("UPDATE")) {
                st.readWord();
                st.readSpaces();
                parseOneTableWithAlias(st, aliasToTable);
                continue;
            }

            if (st.peekWord("DELETE")) {
                st.readWord();
                st.readSpaces();
                if (st.peekWord("FROM")) {
                    st.readWord();
                    st.readSpaces();
                    parseOneTableWithAlias(st, aliasToTable);
                }
                continue;
            }

            if (st.peekWord("INSERT")) {
                st.readWord();
                st.readSpaces();
                if (st.peekWord("INTO")) {
                    st.readWord();
                    st.readSpaces();
                    parseOneTableWithAlias(st, aliasToTable);
                }
                continue;
            }

            if (st.peekWord("MERGE")) {
                st.readWord();
                st.readSpaces();
                if (st.peekWord("INTO")) {
                    st.readWord();
                    st.readSpaces();
                    parseOneTableWithAlias(st, aliasToTable);
                }
                continue;
            }

            st.read();
        }

        return aliasToTable;
    }

    private static void parseFromSources(Scan st, Map<String, String> aliasToTable) {
        while (st.hasNext()) {
            st.readSpaces();
            if (!st.hasNext()) return;

            // boundary at top-level
            if (st.depth == 0 && st.peekIsStopWord()) return;

            // JOIN keywords (LEFT/INNER/... JOIN ...)
            if (st.depth == 0 && st.peekIsJoinWord()) {
                st.readWord(); // LEFT/INNER/...
                st.readSpaces();

                if (st.peekWord("OUTER")) {
                    st.readWord();
                    st.readSpaces();
                }
                if (st.peekWord("JOIN")) {
                    st.readWord();
                    st.readSpaces();
                }
                parseOneSource(st, aliasToTable);
                continue;
            }

            // ---- 핵심: 전진 보장(무한 루프 방지) ----
            int before = st.position();
            parseOneSource(st, aliasToTable);
            int after = st.position();

            // parseOneSource가 아무것도 못 읽은 경우 1글자 강제 소비
            if (after == before) {
                char c = st.peek();
                // 닫는 괄호/세미콜론이면 FROM 소스 파싱 종료로 간주
                if (st.depth == 0 && (c == ')' || c == ';' || c == '\0')) return;
                st.read(); // 1글자 전진
            }

            st.readSpaces();

            // comma separated tables
            if (st.hasNext() && st.peek() == ',') {
                st.read();
                continue;
            }

            if (!st.hasNext()) return;
            if (st.depth == 0 && (st.peekIsJoinWord() || st.peekIsStopWord())) return;
        }
    }

    private static void parseOneSource(Scan st, Map<String, String> aliasToTable) {
        st.readSpaces();
        if (!st.hasNext()) return;

        // derived table: (SELECT ...) ALIAS
        if (st.peek() == '(') {
            st.readParenBlock();
            st.readSpaces();
            if (st.peekIsIdentifier()) {
                st.readWord(); // alias (derived)
            }
            return;
        }

        parseOneTableWithAlias(st, aliasToTable);
    }

    private static void parseOneTableWithAlias(Scan st, Map<String, String> aliasToTable) {
        st.readSpaces();
        if (!st.peekIsIdentifier()) return;

        String tableToken = st.readIdentifier();
        if (tableToken == null || tableToken.isEmpty()) return;

        String table = lastPart(tableToken).toUpperCase();

        // map table name to itself
        aliasToTable.putIfAbsent(table, table);

        st.readSpaces();

        // optional alias
        if (st.peekIsIdentifier()) {
            String next = st.peekWordValue().toUpperCase();

            if (!STOP_WORDS.contains(next)
                    && !JOIN_WORDS.contains(next)
                    && !DML_WORDS.contains(next)
                    && !next.equals("SET")
                    && !next.equals("VALUES")
                    && !next.equals("USING")) {
                String alias = st.readWord().toUpperCase();
                aliasToTable.put(alias, table);
            }
        }
    }

    private static String lastPart(String ident) {
        String t = ident.trim();
        int p = t.lastIndexOf('.');
        return (p >= 0) ? t.substring(p + 1) : t;
    }

    // ------------------------------------------------------------
    // Scanner
    // ------------------------------------------------------------
    private static final class Scan {
        final String s;
        int pos = 0;
        int depth = 0;

        Scan(String s) { this.s = (s == null) ? "" : s; }

        int position() { return pos; }

        boolean hasNext() { return pos < s.length(); }

        char peek() { return (pos < s.length()) ? s.charAt(pos) : '\0'; }

        char read() {
            if (pos >= s.length()) return '\0';
            char c = s.charAt(pos++);
            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);
            return c;
        }

        boolean peekIsLineComment() {
            return pos + 1 < s.length() && s.charAt(pos) == '-' && s.charAt(pos + 1) == '-';
        }

        boolean peekIsBlockComment() {
            return pos + 1 < s.length() && s.charAt(pos) == '/' && s.charAt(pos + 1) == '*';
        }

        boolean peekIsSingleQuotedString() {
            return pos < s.length() && s.charAt(pos) == '\'';
        }

        String readLineComment() {
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '\n') break;
            }
            return "";
        }

        String readBlockComment() {
            if (pos + 1 >= s.length()) return "";
            pos += 2; // /*
            while (pos + 1 < s.length()) {
                if (s.charAt(pos) == '*' && s.charAt(pos + 1) == '/') {
                    pos += 2;
                    break;
                }
                pos++;
            }
            return "";
        }

        String readSingleQuotedString() {
            if (pos >= s.length()) return "";
            pos++; // '
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '\'') {
                    if (pos < s.length() && s.charAt(pos) == '\'') { pos++; continue; }
                    break;
                }
            }
            return "";
        }

        void readSpaces() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        boolean peekWord(String kw) {
            int n = kw.length();
            if (pos + n > s.length()) return false;
            if (pos > 0 && isWordChar(s.charAt(pos - 1))) return false;

            for (int i = 0; i < n; i++) {
                if (Character.toUpperCase(s.charAt(pos + i)) != Character.toUpperCase(kw.charAt(i))) return false;
            }
            if (pos + n < s.length() && isWordChar(s.charAt(pos + n))) return false;
            return true;
        }

        String peekWordValue() {
            int p = pos;
            while (p < s.length() && isWordChar(s.charAt(p))) p++;
            return s.substring(pos, p);
        }

        String readWord() {
            int start = pos;
            while (pos < s.length() && isWordChar(s.charAt(pos))) pos++;
            return s.substring(start, pos);
        }

        boolean peekIsIdentifier() {
            if (pos >= s.length()) return false;
            char c = s.charAt(pos);
            return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
        }

        String readIdentifier() {
            int start = pos;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '$' || c == '#') {
                    pos++;
                    continue;
                }
                break;
            }
            return s.substring(start, pos);
        }

        String readParenBlock() {
            if (peek() != '(') return "";
            int start = pos;
            int d = 0;
            while (pos < s.length()) {
                if (peekIsLineComment()) { readLineComment(); continue; }
                if (peekIsBlockComment()) { readBlockComment(); continue; }
                if (peekIsSingleQuotedString()) { readSingleQuotedString(); continue; }

                char c = read();
                if (c == '(') d++;
                else if (c == ')') {
                    d--;
                    if (d == 0) break;
                }
            }
            return s.substring(start, pos);
        }

        boolean peekIsStopWord() {
            if (!peekIsIdentifier()) return false;
            String w = peekWordValue().toUpperCase();
            return STOP_WORDS.contains(w);
        }

        boolean peekIsJoinWord() {
            if (!peekIsIdentifier()) return false;
            String w = peekWordValue().toUpperCase();
            return JOIN_WORDS.contains(w) || w.equals("JOIN");
        }
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
    }
}
