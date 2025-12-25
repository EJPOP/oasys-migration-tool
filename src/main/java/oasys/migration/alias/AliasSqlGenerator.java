package oasys.migration.alias;

import java.util.Collections;
import java.util.Map;

public class AliasSqlGenerator {

    public enum Mode {
        ASIS_ALIAS_TO_TOBE,
        TOBE_SQL
    }

    private final ColumnMappingRegistry registry;
    private final SelectLineTransformer transformer;

    public AliasSqlGenerator(ColumnMappingRegistry registry) {
        this.registry = registry;
        this.transformer = new SelectLineTransformer(registry);
    }

    public String generate(String sqlText, Mode mode) {
        if (sqlText == null) return "";

        // Resolve aliases on original SQL (used for param rename inference).
        Map<String, String> aliasTableMap0 = FromJoinAliasResolver.resolve(sqlText);

        Map<String, String> paramRenameMap =
                (mode == Mode.TOBE_SQL)
                        ? transformer.buildParamRenameMap(sqlText, aliasTableMap0)
                        : Collections.emptyMap();

        // 1) Transform all SELECT blocks (format + alias/comment rule).
        String out = transformSegment(sqlText, mode, paramRenameMap);

        if (mode == Mode.TOBE_SQL) {
            // 2) Convert the whole SQL (WHERE/ON/HAVING/GROUP/ORDER + DML areas included).
            Map<String, String> aliasTableMap = FromJoinAliasResolver.resolve(out);
            out = transformer.convertSqlFragmentToTobe(out, aliasTableMap, paramRenameMap);

            // 3) (Optional but useful) annotate INSERT/UPDATE column positions with /* tobeName */.
            //     - SELECT list comments are handled in transformSelectBody().
            out = transformer.annotateDml(out, aliasTableMap);

            // ✅ 4) TOBE_SQL 모드에서만: 테이블ID 치환(현행테이블ID -> (TOBE)테이블ID)
            out = convertTableIdsToTobe(out);
        }

        return out;
    }

    /**
     * ✅ TOBE_SQL 모드에서만 사용:
     * column_mapping.xlsx의 "현행테이블ID" -> "테이블ID"로 테이블명을 치환한다.
     * - 문자열/주석은 건드리지 않음
     * - FROM / JOIN / UPDATE / INSERT INTO / MERGE INTO / DELETE FROM 이후의 "테이블 토큰"만 치환
     * - schema.table 형태면 마지막 파트(table)만 치환
     */
    private String convertTableIdsToTobe(String sql) {
        if (sql == null || sql.isEmpty()) return sql;

        StringBuilder out = new StringBuilder(sql.length() + 64);

        TableScan st = new TableScan(sql);

        boolean expectTable = false;
        boolean inFromClause = false;

        boolean seenDelete = false;
        boolean seenInsert = false;
        boolean seenMerge = false;

        while (st.hasNext()) {
            if (st.peekIsLineComment()) { out.append(st.readLineComment()); continue; }
            if (st.peekIsBlockComment()) { out.append(st.readBlockComment()); continue; }
            if (st.peekIsSingleQuotedString()) { out.append(st.readSingleQuotedString()); continue; }
            if (st.peekIsDoubleQuotedString()) { out.append(st.readDoubleQuotedString()); continue; }

            // DELETE FROM
            if (st.peekWord("DELETE")) {
                out.append(st.readWord());
                seenDelete = true;
                expectTable = false;
                inFromClause = false;
                continue;
            }
            if (seenDelete && st.peekWord("FROM")) {
                out.append(st.readWord());
                seenDelete = false;
                expectTable = true;
                inFromClause = true;
                continue;
            }

            // INSERT INTO
            if (st.peekWord("INSERT")) {
                out.append(st.readWord());
                seenInsert = true;
                expectTable = false;
                inFromClause = false;
                continue;
            }
            if (seenInsert && st.peekWord("INTO")) {
                out.append(st.readWord());
                seenInsert = false;
                expectTable = true;
                inFromClause = false;
                continue;
            }

            // MERGE INTO
            if (st.peekWord("MERGE")) {
                out.append(st.readWord());
                seenMerge = true;
                expectTable = false;
                inFromClause = false;
                continue;
            }
            if (seenMerge && st.peekWord("INTO")) {
                out.append(st.readWord());
                seenMerge = false;
                expectTable = true;
                inFromClause = false;
                continue;
            }

            // UPDATE
            if (st.peekWord("UPDATE")) {
                out.append(st.readWord());
                expectTable = true;
                inFromClause = false;
                continue;
            }

            // FROM
            if (st.peekWord("FROM")) {
                out.append(st.readWord());
                expectTable = true;
                inFromClause = true;
                continue;
            }

            // JOIN (LEFT/INNER/... JOIN 포함 - 실제 테이블은 JOIN 다음에 나옴)
            if (st.peekWord("JOIN")) {
                out.append(st.readWord());
                expectTable = true;
                inFromClause = true;
                continue;
            }
            if (st.peekWord("LEFT") || st.peekWord("RIGHT") || st.peekWord("FULL") || st.peekWord("INNER")
                    || st.peekWord("OUTER") || st.peekWord("CROSS")) {
                out.append(st.readWord());
                continue;
            }

            // FROM 절 종료 키워드
            if (inFromClause && (st.peekWord("WHERE") || st.peekWord("GROUP") || st.peekWord("ORDER")
                    || st.peekWord("HAVING") || st.peekWord("UNION") || st.peekWord("INTERSECT")
                    || st.peekWord("EXCEPT") || st.peekWord("MINUS"))) {
                inFromClause = false;
                expectTable = false;
                out.append(st.readWord());
                continue;
            }

            // 테이블 토큰 위치
            if (expectTable) {
                // derived table: (SELECT ...)
                if (st.peek() == '(') {
                    out.append(st.readParenBlock());
                    expectTable = false;
                    continue;
                }

                if (isIdentStart(st.peek())) {
                    String tableToken = st.readIdentifier(); // schema.table 가능
                    out.append(replaceTableTokenToTobe(tableToken));
                    expectTable = false;
                    continue;
                }

                // 공백/개행 등은 그대로 흘림
                out.append(st.read());
                continue;
            }

            // FROM 절에서 콤마 다음도 테이블 기대
            if (inFromClause && st.peek() == ',') {
                out.append(st.read());
                expectTable = true;
                continue;
            }

            out.append(st.read());
        }

        return out.toString();
    }

    private String replaceTableTokenToTobe(String rawTableToken) {
        if (rawTableToken == null || rawTableToken.isBlank()) return rawTableToken;

        int dot = rawTableToken.lastIndexOf('.');
        String prefix = (dot >= 0) ? rawTableToken.substring(0, dot + 1) : "";
        String last = (dot >= 0) ? rawTableToken.substring(dot + 1) : rawTableToken;

        String mapped = registry.findTobeTableId(last);
        if (mapped == null || mapped.isBlank()) return rawTableToken;

        return prefix + mapped;
    }

    private static boolean isIdentStart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
    }

    private String transformSegment(String sql, Mode mode, Map<String, String> paramRenameMap) {
        if (sql == null || sql.isEmpty()) return sql;

        StringBuilder sb = new StringBuilder(sql.length() + 64);

        int i = 0;
        while (i < sql.length()) {
            SelectHit hit = findNextSelect(sql, i);
            if (hit == null) {
                sb.append(sql, i, sql.length());
                break;
            }

            // copy prefix
            sb.append(sql, i, hit.selectStart);

            int end = findSelectEnd(sql, hit.selectStart, hit.depthAtSelect);
            if (end <= hit.selectStart) {
                // fallback: append the rest
                sb.append(sql, hit.selectStart, sql.length());
                break;
            }

            String stmt = sql.substring(hit.selectStart, end);
            String transformed = transformSelectStatement(stmt, mode, paramRenameMap);
            sb.append(transformed);

            i = end;
        }

        return sb.toString();
    }

    private String transformSelectStatement(String stmt, Mode mode, Map<String, String> paramRenameMap) {
        // stmt starts with SELECT (case-insensitive), but may contain leading spaces/newlines.
        int selectIdx = indexOfKeywordOutside(stmt, 0, "SELECT");
        if (selectIdx < 0) return stmt;

        int afterSelect = selectIdx + 6; // len("SELECT")

        int fromIdx = indexOfKeywordOutside(stmt, afterSelect, "FROM");
        if (fromIdx < 0) return stmt;

        String prefix = stmt.substring(0, afterSelect); // keep original casing/spaces
        String selectBody = stmt.substring(afterSelect, fromIdx);
        String rest = stmt.substring(fromIdx);

        // Recursively transform nested SELECTs inside selectBody/rest.
        String selectBodyNested = transformSegment(selectBody, mode, paramRenameMap);
        String restNested = transformSegment(rest, mode, paramRenameMap);

        // Resolve table aliases for this SELECT (based on rest part).
        Map<String, String> aliasTableMap = FromJoinAliasResolver.resolve(restNested);

        // Apply select-list rule (mode dependent).
        String newSelectBody = transformer.transformSelectBody(selectBodyNested, aliasTableMap, mode);

        // In TOBE_SQL mode: do not rebuild WHERE/ON/GROUP/ORDER here; we convert the whole SQL later.
        return prefix + newSelectBody + restNested;
    }

    // ----------------------------
    // SELECT boundary detection
    // ----------------------------

    private static final class SelectHit {
        final int selectStart;
        final int depthAtSelect;

        SelectHit(int selectStart, int depthAtSelect) {
            this.selectStart = selectStart;
            this.depthAtSelect = depthAtSelect;
        }
    }

    private SelectHit findNextSelect(String s, int start) {
        ScanState st = new ScanState();

        // initialize state by scanning up to 'start'
        for (int i = 0; i < start && i < s.length(); i++) {
            st.step(s, i);
        }

        for (int i = start; i < s.length(); i++) {
            if (!st.step(s, i)) continue;

            if (isKeywordAt(s, i, "SELECT")) {
                return new SelectHit(i, st.depth);
            }
        }
        return null;
    }

    private int findSelectEnd(String s, int selectStart, int baseDepth) {
        ScanState st = new ScanState();

        // initialize state up to selectStart
        for (int i = 0; i < selectStart && i < s.length(); i++) {
            st.step(s, i);
        }

        // scan after selectStart
        for (int i = selectStart + 6; i < s.length(); i++) {
            st.step(s, i);

            // leaving the base depth (closing parenthesis)
            if (!st.inStringOrComment() && st.depth < baseDepth) {
                return i;
            }

            if (!st.inStringOrComment() && st.depth == baseDepth) {
                // UNION boundary at same level -> end current select statement
                if (isKeywordAt(s, i, "UNION")
                        || isKeywordAt(s, i, "INTERSECT")
                        || isKeywordAt(s, i, "EXCEPT")
                        || isKeywordAt(s, i, "MINUS")) {
                    return i;
                }
                if (s.charAt(i) == ';') return i;
            }
        }
        return s.length();
    }

    private int indexOfKeywordOutside(String s, int start, String kw) {
        ScanState st = new ScanState();
        for (int i = 0; i < start && i < s.length(); i++) st.step(s, i);

        for (int i = start; i < s.length(); i++) {
            st.step(s, i);
            if (st.inStringOrComment()) continue;
            if (isKeywordAt(s, i, kw)) return i;
        }
        return -1;
    }

    private boolean isKeywordAt(String s, int idx, String kw) {
        int n = kw.length();
        if (idx < 0 || idx + n > s.length()) return false;

        // word boundary before
        if (idx > 0 && isWordChar(s.charAt(idx - 1))) return false;

        for (int i = 0; i < n; i++) {
            char a = s.charAt(idx + i);
            char b = kw.charAt(i);
            if (Character.toUpperCase(a) != Character.toUpperCase(b)) return false;
        }

        // word boundary after
        if (idx + n < s.length() && isWordChar(s.charAt(idx + n))) return false;

        return true;
    }

    private boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
    }

    // ----------------------------
    // Scanner state
    // ----------------------------

    private static final class ScanState {
        int depth = 0;

        boolean inSingle = false;
        boolean inDouble = false;

        boolean inLineComment = false;
        boolean inBlockComment = false;

        boolean inStringOrComment() {
            return inSingle || inDouble || inLineComment || inBlockComment;
        }

        boolean step(String s, int i) {
            char c = s.charAt(i);
            char n = (i + 1 < s.length()) ? s.charAt(i + 1) : '\0';

            // line comment end
            if (inLineComment) {
                if (c == '\n') inLineComment = false;
                return false;
            }

            // block comment end
            if (inBlockComment) {
                if (c == '*' && n == '/') {
                    inBlockComment = false;
                }
                return false;
            }

            // string handling
            if (inSingle) {
                if (c == '\'' && n == '\'') {
                    // escaped single quote
                    return false;
                }
                if (c == '\'') inSingle = false;
                return false;
            }
            if (inDouble) {
                if (c == '"') inDouble = false;
                return false;
            }

            // comment start
            if (c == '-' && n == '-') {
                inLineComment = true;
                return false;
            }
            if (c == '/' && n == '*') {
                inBlockComment = true;
                return false;
            }

            // string start
            if (c == '\'') {
                inSingle = true;
                return false;
            }
            if (c == '"') {
                inDouble = true;
                return false;
            }

            // depth
            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);

            return true;
        }
    }

    /**
     * 테이블 토큰만 안전하게 읽기 위한 간단 스캐너 (문자열/주석 무시)
     */
    private static final class TableScan {
        final String s;
        int pos = 0;

        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inSingle = false;
        boolean inDouble = false;

        TableScan(String s) { this.s = (s == null) ? "" : s; }

        boolean hasNext() { return pos < s.length(); }

        char peek() { return (pos < s.length()) ? s.charAt(pos) : '\0'; }

        char read() { return (pos < s.length()) ? s.charAt(pos++) : '\0'; }

        boolean peekIsLineComment() {
            return !inSingle && !inDouble && !inBlockComment
                    && pos + 1 < s.length() && s.charAt(pos) == '-' && s.charAt(pos + 1) == '-';
        }

        boolean peekIsBlockComment() {
            return !inSingle && !inDouble && !inLineComment
                    && pos + 1 < s.length() && s.charAt(pos) == '/' && s.charAt(pos + 1) == '*';
        }

        boolean peekIsSingleQuotedString() {
            return !inDouble && !inLineComment && !inBlockComment
                    && pos < s.length() && s.charAt(pos) == '\'';
        }

        boolean peekIsDoubleQuotedString() {
            return !inSingle && !inLineComment && !inBlockComment
                    && pos < s.length() && s.charAt(pos) == '"';
        }

        String readLineComment() {
            int start = pos;
            pos += 2; // --
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '\n') break;
            }
            return s.substring(start, pos);
        }

        String readBlockComment() {
            int start = pos;
            pos += 2; // /*
            while (pos + 1 < s.length()) {
                if (s.charAt(pos) == '*' && s.charAt(pos + 1) == '/') {
                    pos += 2;
                    break;
                }
                pos++;
            }
            return s.substring(start, pos);
        }

        String readSingleQuotedString() {
            int start = pos;
            pos++; // '
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '\'') {
                    if (pos < s.length() && s.charAt(pos) == '\'') { pos++; continue; } // ''
                    break;
                }
            }
            return s.substring(start, pos);
        }

        String readDoubleQuotedString() {
            int start = pos;
            pos++; // "
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '"') break;
            }
            return s.substring(start, pos);
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

        String readWord() {
            int start = pos;
            while (pos < s.length() && isWordChar(s.charAt(pos))) pos++;
            return s.substring(start, pos);
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
                if (peekIsDoubleQuotedString()) { readDoubleQuotedString(); continue; }

                char c = read();
                if (c == '(') d++;
                else if (c == ')') {
                    d--;
                    if (d == 0) break;
                }
            }
            return s.substring(start, pos);
        }

        private static boolean isWordChar(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
        }
    }
}
