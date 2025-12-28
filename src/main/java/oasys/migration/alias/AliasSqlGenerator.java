package oasys.migration.alias;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;

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

            // 3) annotate INSERT/UPDATE column positions with /* tobeName */.
            out = transformer.annotateDml(out, aliasTableMap);

            // ✅ 3.5) WHERE/ON/... 에 별칭 없는 컬럼이 남는 케이스 fallback 보정
            out = convertBareColumnsInClausesToTobe(out);

            // ✅ 4) TOBE_SQL 모드에서만: 테이블ID 치환(현행테이블ID -> (TOBE)테이블ID)
            out = convertTableIdsToTobe(out);
        }

        // ✅ (FIX) "SYSDATEWHERE" 같은 붙은 토큰은 prettifier 전에 먼저 쪼개야 절 인식이 정상 동작함
        out = fixGluedKeywordsAfterSpecialTokens(out);

        if (Boolean.parseBoolean(System.getProperty("oasys.migration.pretty", "false"))) {
            out = SqlPrettifier.format(out);
        }

        // ✅ (안전망) prettifier 이후에도 혹시 남아있다면 한 번 더 (대부분 idempotent)
        out = fixGluedKeywordsAfterSpecialTokens(out);

        return out;
    }

    // ============================================================
    // ✅ (NEW) SYSDATEWHERE / SYSDATEAND ... 같이 붙어서 나오는 키워드 보정
    //  - WHERE 같은 "절 키워드"는 줄바꿈으로 아래로 내려가도록 처리
    // ============================================================

    /**
     * 변환/포매팅 과정에서 공백/개행이 유실되어
     * SYSDATEWHERE, SYSDATEAND, CURRENT_TIMESTAMPWHERE 같은 토큰이 생기는 경우가 있음.
     *
     * - 주석/문자열/MyBatis 파라미터는 그대로 보존
     * - "특정 안전 prefix 토큰" 뒤에 SQL 키워드가 붙으면 분리
     *   * WHERE/FROM/GROUP/ORDER/HAVING/UNION... 같은 절 키워드는 "\n"로 분리(아래로 내려감)
     *   * AND/OR/ON/BY 같은 접속/보조 키워드는 " "로 분리
     */
    private String fixGluedKeywordsAfterSpecialTokens(String sql) {
        if (sql == null || sql.isEmpty()) return sql;

        StringBuilder out = new StringBuilder(sql.length() + 64);
        SqlScan st = new SqlScan(sql);

        while (st.hasNext()) {
            // preserve (절대 변형 금지 영역)
            if (st.peekIsLineComment()) { out.append(st.readLineComment()); continue; }
            if (st.peekIsBlockComment()) { out.append(st.readBlockComment()); continue; }
            if (st.peekIsSingleQuotedString()) { out.append(st.readSingleQuotedString()); continue; }
            if (st.peekIsDoubleQuotedString()) { out.append(st.readDoubleQuotedString()); continue; }
            if (st.peekIsMyBatisParam()) { out.append(st.readMyBatisParam()); continue; }
            if (st.peekIsHashToken()) { out.append(st.readHashToken()); continue; }

            char ch = st.peek();

            if (isIdentStart(ch)) {
                String word = st.readWord();
                if (!word.isEmpty()) {
                    out.append(splitGluedTokenIfNeeded(word));
                    continue;
                }
            }

            out.append(st.read());
        }

        return out.toString();
    }

    /**
     * "안전 prefix" + "키워드"가 한 단어로 붙은 토큰을 분리한다.
     * - 토큰이 안전 prefix 로 시작하고,
     * - 이어지는 문자열이 특정 키워드로 시작하면,
     * - prefix / keyword 사이를 (절 키워드면 개행, 아니면 공백)으로 분리한다.
     */
    private static String splitGluedTokenIfNeeded(String token) {
        if (token == null || token.isEmpty()) return token;

        String up = token.toUpperCase(Locale.ROOT);

        for (String prefixUp : GLUE_SAFE_PREFIXES_UPPER) {
            if (!up.startsWith(prefixUp)) continue;
            if (up.length() <= prefixUp.length()) continue;

            String restUp = up.substring(prefixUp.length());

            for (String kwUp : GLUE_KEYWORDS_UPPER_SORTED) {
                if (!restUp.startsWith(kwUp)) continue;

                int pLen = prefixUp.length();
                int kLen = kwUp.length();

                // 원문 케이스 유지
                String pRaw = token.substring(0, Math.min(pLen, token.length()));
                String kRaw = token.substring(Math.min(pLen, token.length()),
                        Math.min(pLen + kLen, token.length()));
                String rem = token.substring(Math.min(pLen + kLen, token.length()));

                // ✅ WHERE/FROM/GROUP/ORDER/HAVING/UNION... 같은 절 키워드는 줄바꿈으로 "아래로"
                String sep1 = isClauseNewlineKeyword(kwUp) ? "\n" : " ";

                // keyword 다음은 rem이 있으면 공백 1개
                if (rem == null || rem.isEmpty()) {
                    return pRaw + sep1 + kRaw;
                }
                return pRaw + sep1 + kRaw + " " + rem;
            }
        }

        return token;
    }

    /**
     * 절(Clause) 시작 성격의 키워드: 앞 토큰과 붙으면 줄바꿈으로 내려보내는 게 안전함.
     */
    private static boolean isClauseNewlineKeyword(String kwUp) {
        if (kwUp == null) return false;
        switch (kwUp) {
            case "WHERE":
            case "FROM":
            case "GROUP":
            case "ORDER":
            case "HAVING":
            case "UNION":
            case "INTERSECT":
            case "EXCEPT":
            case "MINUS":
            case "VALUES":
            case "SET":
            case "JOIN":
            case "INTO":
                return true;
            default:
                return false;
        }
    }

    /**
     * 공백 유실 시 "앞 토큰"으로 자주 등장하고, 뒤에 절 키워드가 붙으면 거의 100% 오류인 토큰들
     * (필요 시 여기만 늘리면 됨)
     */
    private static final String[] GLUE_SAFE_PREFIXES_UPPER = new String[] {
            "SYSDATE",
            "SYSTIMESTAMP",
            "LOCALTIMESTAMP",
            "CURRENT_DATE",
            "CURRENT_TIMESTAMP",
            "CURRENT_TIME",
            "NOW",
            "GETDATE",
            "END"
    };

    /**
     * 붙어서 나오면 깨지는 주요 키워드들(길이 긴 것 우선)
     * - WHERE 뿐 아니라 GROUP/ORDER(+BY) 등도 같이 보정
     */
    private static final String[] GLUE_KEYWORDS_UPPER_SORTED = new String[] {
            "INTERSECT",
            "RETURNING",
            "HAVING",
            "WHERE",
            "GROUP",
            "ORDER",
            "UNION",
            "EXCEPT",
            "MINUS",
            "FROM",
            "VALUES",
            "JOIN",
            "INTO",
            "WHEN",
            "THEN",
            "ELSE",
            "SET",
            "AND",
            "OR",
            "ON",
            "BY"
    };

    // ============================================================
    // ✅ (NEW) WHERE/ON/HAVING/GROUP BY/ORDER BY/SET 내 "별칭 없는 컬럼ID" fallback 치환
    // ============================================================

    private String convertBareColumnsInClausesToTobe(String sql) {
        if (sql == null || sql.isEmpty()) return sql;

        // FROM/JOIN 기준 ASIS 테이블 후보 수집 (테이블명이 이미 tobe로 바뀌기 전 단계에서 수행)
        List<String> asisTables = collectAsisTableIds(sql);
        if (asisTables.isEmpty()) return sql;

        StringBuilder out = new StringBuilder(sql.length() + 64);

        Clause clause = Clause.NONE;
        boolean pendingGroup = false;
        boolean pendingOrder = false;

        SqlScan st = new SqlScan(sql);
        int depth = 0;

        while (st.hasNext()) {
            // preserve
            if (st.peekIsLineComment()) { out.append(st.readLineComment()); continue; }
            if (st.peekIsBlockComment()) { out.append(st.readBlockComment()); continue; }
            if (st.peekIsSingleQuotedString()) { out.append(st.readSingleQuotedString()); continue; }
            if (st.peekIsDoubleQuotedString()) { out.append(st.readDoubleQuotedString()); continue; }
            if (st.peekIsMyBatisParam()) { out.append(st.readMyBatisParam()); continue; }
            if (st.peekIsHashToken()) { out.append(st.readHashToken()); continue; }

            char ch = st.peek();

            // depth
            if (ch == '(') {
                // (SELECT ...) 같은 서브쿼리는 통째로 copy (변환은 내부 transformSegment/convertSqlFragmentToTobe가 담당)
                if (st.peekParenStartsWithSelect()) {
                    out.append(st.readParenBlock());
                    continue;
                }
                depth++;
                out.append(st.read());
                continue;
            }
            if (ch == ')') {
                depth = Math.max(0, depth - 1);
                out.append(st.read());
                continue;
            }

            // word
            if (isIdentStart(ch)) {
                String word = st.readWord();
                if (word.isEmpty()) continue;

                String u = word.toUpperCase(Locale.ROOT);

                // GROUP BY / ORDER BY
                if (pendingGroup) {
                    pendingGroup = false;
                    if ("BY".equals(u) && depth == 0) clause = Clause.GROUP_BY;
                    out.append(word);
                    continue;
                }
                if (pendingOrder) {
                    pendingOrder = false;
                    if ("BY".equals(u) && depth == 0) clause = Clause.ORDER_BY;
                    out.append(word);
                    continue;
                }

                // clause enter/exit (top-level에서만 안정적으로 전환)
                if (depth == 0) {
                    if ("WHERE".equals(u)) { clause = Clause.WHERE; out.append(word); continue; }
                    if ("ON".equals(u)) { clause = Clause.ON; out.append(word); continue; }
                    if ("HAVING".equals(u)) { clause = Clause.HAVING; out.append(word); continue; }
                    if ("SET".equals(u)) { clause = Clause.SET; out.append(word); continue; }

                    if ("GROUP".equals(u)) { pendingGroup = true; out.append(word); continue; }
                    if ("ORDER".equals(u)) { pendingOrder = true; out.append(word); continue; }

                    // boundary: 새로운 큰 절로 넘어가면 clause 종료
                    if (isClauseBoundaryKeyword(u)) {
                        clause = Clause.NONE;
                        out.append(word);
                        continue;
                    }

                    // JOIN 나오면 ON 절 종료로도 간주
                    if ("JOIN".equals(u) && clause == Clause.ON) {
                        clause = Clause.NONE;
                        out.append(word);
                        continue;
                    }
                }

                // qualified identifier (alias.col or schema.col) 는 이미 transformer 쪽에서 처리 영역 => 그대로 둠
                if (st.peek() == '.') {
                    out.append(word);
                    continue;
                }

                // fallback 치환 대상
                if (isTargetClause(clause) && looksLikeAsisColumnId(word)) {
                    String mapped = resolveTobeColumnFromTables(asisTables, word);
                    if (mapped != null && !mapped.isBlank()) {
                        out.append(mapped);
                        continue;
                    }
                }

                out.append(word);
                continue;
            }

            out.append(st.read());
        }

        return out.toString();
    }

    private enum Clause {
        NONE, WHERE, ON, HAVING, GROUP_BY, ORDER_BY, SET
    }

    private static boolean isTargetClause(Clause c) {
        return c == Clause.WHERE || c == Clause.ON || c == Clause.HAVING
                || c == Clause.GROUP_BY || c == Clause.ORDER_BY || c == Clause.SET;
    }

    private static boolean isClauseBoundaryKeyword(String u) {
        return "FROM".equals(u)
                || "UNION".equals(u) || "INTERSECT".equals(u) || "EXCEPT".equals(u) || "MINUS".equals(u)
                || "CONNECT".equals(u) || "START".equals(u) || "FETCH".equals(u) || "FOR".equals(u);
    }

    /**
     * ✅ ASIS 컬럼ID처럼 보이는지(보수적)
     * - 대문자/숫자/_ 로만 구성
     * - '_' 포함
     */
    private static boolean looksLikeAsisColumnId(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.isEmpty()) return false;
        if (t.length() > 80) return false;

        if (t.indexOf('_') < 0) return false;

        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (!(Character.isUpperCase(c) || Character.isDigit(c) || c == '_')) return false;
        }
        return true;
    }

    /**
     * 테이블 후보들 기준으로 컬럼 매핑이 유일할 때만 치환
     * - 이미 TOBE 컬럼으로 인식되는 경우(asisTable + tobeCol)면 안전하게 스킵
     */
    private String resolveTobeColumnFromTables(List<String> asisTables, String asisCol) {
        if (asisTables == null || asisTables.isEmpty()) return null;
        if (asisCol == null || asisCol.isBlank()) return null;

        // 이미 tobe 컬럼이면 건드리지 않음
        for (String t : asisTables) {
            if (t == null || t.isBlank()) continue;
            if (registry.findByTobeOnAsisTable(t, asisCol) != null) {
                return null;
            }
        }

        String found = null;

        for (String t : asisTables) {
            if (t == null || t.isBlank()) continue;

            ColumnMapping m = registry.find(t, asisCol);
            if (m == null) continue;

            String tobe = (m.tobeColumnId == null) ? null : m.tobeColumnId.trim();
            if (tobe == null || tobe.isBlank()) continue;

            if (found == null) {
                found = tobe;
            } else if (!found.equalsIgnoreCase(tobe)) {
                // 2개 이상 서로 다른 매핑이면 모호 -> 치환 안함
                return null;
            }
        }

        return found;
    }

    /**
     * FROM/JOIN 기준 ASIS 테이블ID 후보 수집 (schema.table이면 table만)
     */
    private List<String> collectAsisTableIds(String sql) {
        if (sql == null || sql.isEmpty()) return Collections.emptyList();

        LinkedHashSet<String> set = new LinkedHashSet<>();
        TableScan st = new TableScan(sql);

        boolean expectTable = false;
        boolean inFromClause = false;

        boolean seenDelete = false;
        boolean seenInsert = false;
        boolean seenMerge = false;

        while (st.hasNext()) {
            if (st.peekIsLineComment()) { st.readLineComment(); continue; }
            if (st.peekIsBlockComment()) { st.readBlockComment(); continue; }
            if (st.peekIsSingleQuotedString()) { st.readSingleQuotedString(); continue; }
            if (st.peekIsDoubleQuotedString()) { st.readDoubleQuotedString(); continue; }

            // DELETE FROM
            if (st.peekWord("DELETE")) {
                st.readWord();
                seenDelete = true;
                expectTable = false;
                inFromClause = false;
                continue;
            }
            if (seenDelete && st.peekWord("FROM")) {
                st.readWord();
                seenDelete = false;
                expectTable = true;
                inFromClause = true;
                continue;
            }

            // INSERT INTO
            if (st.peekWord("INSERT")) {
                st.readWord();
                seenInsert = true;
                expectTable = false;
                inFromClause = false;
                continue;
            }
            if (seenInsert && st.peekWord("INTO")) {
                st.readWord();
                seenInsert = false;
                expectTable = true;
                inFromClause = false;
                continue;
            }

            // MERGE INTO
            if (st.peekWord("MERGE")) {
                st.readWord();
                seenMerge = true;
                expectTable = false;
                inFromClause = false;
                continue;
            }
            if (seenMerge && st.peekWord("INTO")) {
                st.readWord();
                seenMerge = false;
                expectTable = true;
                inFromClause = false;
                continue;
            }

            // UPDATE
            if (st.peekWord("UPDATE")) {
                st.readWord();
                expectTable = true;
                inFromClause = false;
                continue;
            }

            // FROM
            if (st.peekWord("FROM")) {
                st.readWord();
                expectTable = true;
                inFromClause = true;
                continue;
            }

            // JOIN
            if (st.peekWord("JOIN")) {
                st.readWord();
                expectTable = true;
                inFromClause = true;
                continue;
            }
            if (st.peekWord("LEFT") || st.peekWord("RIGHT") || st.peekWord("FULL") || st.peekWord("INNER")
                    || st.peekWord("OUTER") || st.peekWord("CROSS")) {
                st.readWord();
                continue;
            }

            // FROM 종료 키워드
            if (inFromClause && (st.peekWord("WHERE") || st.peekWord("GROUP") || st.peekWord("ORDER")
                    || st.peekWord("HAVING") || st.peekWord("UNION") || st.peekWord("INTERSECT")
                    || st.peekWord("EXCEPT") || st.peekWord("MINUS"))) {
                inFromClause = false;
                expectTable = false;
                st.readWord();
                continue;
            }

            if (expectTable) {
                if (st.peek() == '(') {
                    st.readParenBlock();
                    expectTable = false;
                    continue;
                }

                if (isIdentStart(st.peek())) {
                    String tok = st.readIdentifier(); // schema.table
                    String last = lastPart(tok);
                    if (last != null && !last.isBlank()) set.add(last.toUpperCase(Locale.ROOT));
                    expectTable = false;
                    continue;
                }

                st.read();
                continue;
            }

            if (inFromClause && st.peek() == ',') {
                st.read();
                expectTable = true;
                continue;
            }

            st.read();
        }

        return new ArrayList<>(set);
    }

    private static String lastPart(String schemaDotName) {
        if (schemaDotName == null || schemaDotName.isBlank()) return null;
        String t = schemaDotName.trim();
        int dot = t.lastIndexOf('.');
        return (dot >= 0) ? t.substring(dot + 1) : t;
    }

    private static boolean isIdentStart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
    }

    // ============================================================
    // ✅ TOBE_SQL 모드에서만 사용: 테이블ID 치환(현행테이블ID -> (TOBE)테이블ID)
    // ============================================================

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

            // JOIN
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

            if (expectTable) {
                if (st.peek() == '(') {
                    out.append(st.readParenBlock());
                    expectTable = false;
                    continue;
                }

                if (isIdentStart(st.peek())) {
                    String tableToken = st.readIdentifier();
                    out.append(replaceTableTokenToTobe(tableToken));
                    expectTable = false;
                    continue;
                }

                out.append(st.read());
                continue;
            }

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

            sb.append(sql, i, hit.selectStart);

            int end = findSelectEnd(sql, hit.selectStart, hit.depthAtSelect);
            if (end <= hit.selectStart) {
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
        int selectIdx = indexOfKeywordOutside(stmt, 0, "SELECT");
        if (selectIdx < 0) return stmt;

        int afterSelect = selectIdx + 6;
        int fromIdx = indexOfKeywordOutsideTopLevel(stmt, afterSelect, "FROM");
        if (fromIdx < 0) return stmt;

        String prefix = stmt.substring(0, afterSelect);
        String selectBody = stmt.substring(afterSelect, fromIdx);
        String rest = stmt.substring(fromIdx);

        String selectBodyNested = transformSegment(selectBody, mode, paramRenameMap);
        String restNested = transformSegment(rest, mode, paramRenameMap);

        Map<String, String> aliasTableMap = FromJoinAliasResolver.resolve(restNested);

        String newSelectBody = transformer.transformSelectBody(selectBodyNested, aliasTableMap, mode);
        return prefix + newSelectBody + restNested;
    }

    private int indexOfKeywordOutsideTopLevel(String s, int start, String kw) {
        ScanState st = new ScanState();
        for (int i = 0; i < start && i < s.length(); i++) st.step(s, i);

        for (int i = start; i < s.length(); i++) {
            st.step(s, i);
            if (st.inStringOrComment()) continue;

            // ✅ 핵심: 괄호 depth==0(=메인 SELECT 레벨)에서만 FROM을 인정
            if (st.depth != 0) continue;

            if (isKeywordAt(s, i, kw)) return i;
        }
        return -1;
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

        for (int i = 0; i < selectStart && i < s.length(); i++) {
            st.step(s, i);
        }

        for (int i = selectStart + 6; i < s.length(); i++) {
            st.step(s, i);

            if (!st.inStringOrComment() && st.depth < baseDepth) {
                return i;
            }

            if (!st.inStringOrComment() && st.depth == baseDepth) {
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

        if (idx > 0 && isWordChar(s.charAt(idx - 1))) return false;

        for (int i = 0; i < n; i++) {
            char a = s.charAt(idx + i);
            char b = kw.charAt(i);
            if (Character.toUpperCase(a) != Character.toUpperCase(b)) return false;
        }

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

            if (inLineComment) {
                if (c == '\n') inLineComment = false;
                return false;
            }

            if (inBlockComment) {
                if (c == '*' && n == '/') {
                    inBlockComment = false;
                }
                return false;
            }

            if (inSingle) {
                if (c == '\'' && n == '\'') {
                    return false;
                }
                if (c == '\'') inSingle = false;
                return false;
            }
            if (inDouble) {
                if (c == '"') inDouble = false;
                return false;
            }

            if (c == '-' && n == '-') {
                inLineComment = true;
                return false;
            }
            if (c == '/' && n == '*') {
                inBlockComment = true;
                return false;
            }

            if (c == '\'') {
                inSingle = true;
                return false;
            }
            if (c == '"') {
                inDouble = true;
                return false;
            }

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

        TableScan(String s) { this.s = (s == null) ? "" : s; }

        boolean hasNext() { return pos < s.length(); }
        char peek() { return (pos < s.length()) ? s.charAt(pos) : '\0'; }
        char read() { return (pos < s.length()) ? s.charAt(pos++) : '\0'; }

        boolean peekIsLineComment() {
            return pos + 1 < s.length() && s.charAt(pos) == '-' && s.charAt(pos + 1) == '-';
        }

        boolean peekIsBlockComment() {
            return pos + 1 < s.length() && s.charAt(pos) == '/' && s.charAt(pos + 1) == '*';
        }

        boolean peekIsSingleQuotedString() {
            return pos < s.length() && s.charAt(pos) == '\'';
        }

        boolean peekIsDoubleQuotedString() {
            return pos < s.length() && s.charAt(pos) == '"';
        }

        String readLineComment() {
            int start = pos;
            pos += 2;
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '\n') break;
            }
            return s.substring(start, pos);
        }

        String readBlockComment() {
            int start = pos;
            pos += 2;
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
            pos++;
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '\'') {
                    if (pos < s.length() && s.charAt(pos) == '\'') { pos++; continue; }
                    break;
                }
            }
            return s.substring(start, pos);
        }

        String readDoubleQuotedString() {
            int start = pos;
            pos++;
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

    /**
     * ✅ WHERE/ON/... 구간 fallback을 위한 SQL 스캐너(주석/문자열/파라미터 보존)
     */
    private static final class SqlScan {
        final String s;
        int pos = 0;

        SqlScan(String s) { this.s = (s == null) ? "" : s; }

        boolean hasNext() { return pos < s.length(); }
        char peek() { return (pos < s.length()) ? s.charAt(pos) : '\0'; }
        char read() { return (pos < s.length()) ? s.charAt(pos++) : '\0'; }

        boolean peekIsLineComment() { return pos + 1 < s.length() && s.charAt(pos) == '-' && s.charAt(pos + 1) == '-'; }
        boolean peekIsBlockComment() { return pos + 1 < s.length() && s.charAt(pos) == '/' && s.charAt(pos + 1) == '*'; }
        boolean peekIsSingleQuotedString() { return pos < s.length() && s.charAt(pos) == '\''; }
        boolean peekIsDoubleQuotedString() { return pos < s.length() && s.charAt(pos) == '"'; }

        boolean peekIsMyBatisParam() {
            if (pos + 1 >= s.length()) return false;
            char c = s.charAt(pos);
            char n = s.charAt(pos + 1);
            return (c == '#' && n == '{') || (c == '$' && n == '{');
        }

        boolean peekIsHashToken() {
            if (pos >= s.length()) return false;
            if (s.charAt(pos) != '#') return false;
            if (pos + 1 < s.length() && s.charAt(pos + 1) == '{') return false;
            int next = s.indexOf('#', pos + 1);
            return next > pos + 1;
        }

        String readLineComment() {
            int start = pos;
            pos += 2;
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '\n') break;
            }
            return s.substring(start, pos);
        }

        String readBlockComment() {
            int start = pos;
            pos += 2;
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
            pos++;
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '\'') {
                    if (pos < s.length() && s.charAt(pos) == '\'') { pos++; continue; }
                    break;
                }
            }
            return s.substring(start, pos);
        }

        String readDoubleQuotedString() {
            int start = pos;
            pos++;
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '"') break;
            }
            return s.substring(start, pos);
        }

        String readMyBatisParam() {
            int start = pos;
            pos += 2; // #{ or ${
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '}') break;
            }
            return s.substring(start, pos);
        }

        String readHashToken() {
            int start = pos;
            pos++;
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '#') break;
            }
            return s.substring(start, pos);
        }

        String readWord() {
            int start = pos;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#') {
                    pos++;
                    continue;
                }
                break;
            }
            return s.substring(start, pos);
        }

        // ---- (SELECT ...) 서브쿼리 판별/복사용
        boolean peekParenStartsWithSelect() {
            if (peek() != '(') return false;

            int i = pos + 1;
            int n = s.length();

            while (i < n) {
                // ws
                while (i < n && Character.isWhitespace(s.charAt(i))) i++;

                // -- comment
                if (i + 1 < n && s.charAt(i) == '-' && s.charAt(i + 1) == '-') {
                    i += 2;
                    while (i < n && s.charAt(i) != '\n') i++;
                    continue;
                }

                // /* comment */
                if (i + 1 < n && s.charAt(i) == '/' && s.charAt(i + 1) == '*') {
                    i += 2;
                    while (i + 1 < n) {
                        if (s.charAt(i) == '*' && s.charAt(i + 1) == '/') { i += 2; break; }
                        i++;
                    }
                    continue;
                }

                break;
            }

            // SELECT keyword
            String kw = "SELECT";
            if (i + kw.length() > n) return false;

            for (int k = 0; k < kw.length(); k++) {
                if (Character.toUpperCase(s.charAt(i + k)) != kw.charAt(k)) return false;
            }

            int j = i + kw.length();
            if (j < n) {
                char c = s.charAt(j);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#') return false;
            }

            return true;
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
                if (peekIsMyBatisParam()) { readMyBatisParam(); continue; }
                if (peekIsHashToken()) { readHashToken(); continue; }

                char c = read();
                if (c == '(') d++;
                else if (c == ')') {
                    d--;
                    if (d == 0) break;
                }
            }

            return s.substring(start, pos);
        }
    }
}
