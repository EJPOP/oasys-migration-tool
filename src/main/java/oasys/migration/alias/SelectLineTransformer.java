package oasys.migration.alias;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SelectLineTransformer {

    private final ColumnMappingRegistry registry;

    public SelectLineTransformer(ColumnMappingRegistry registry) {
        this.registry = registry;
    }

    /**
     * Transform SELECT list body (between SELECT and FROM).
     *
     * Mode rules:
     * - ASIS_ALIAS_TO_TOBE:
     *      ✅ 변경됨: ASIS_COL(or ASIS expr) AS TOBE_COL
     *      (ASIS 모드에서는 SELECT expr 자체는 ASIS로 유지, alias만 TOBE로 건다)
     *
     * - TOBE_SQL:
     *      - direct column ref -> output only:  TOBE_COL (alias 없음)
     *      - expression -> EXPR_TO_TOBE AS TOBE_COL
     */
    public String transformSelectBody(String selectBody,
                                      Map<String, String> aliasTableMap,
                                      AliasSqlGenerator.Mode mode) {

        List<SelectItem> items = splitSelectItems(selectBody);
        if (items.isEmpty()) {
            return selectBody;
        }

        StringBuilder out = new StringBuilder(selectBody.length() + 128);

        for (int i = 0; i < items.size(); i++) {
            SelectItem it = items.get(i);

            String commentStripped = stripTrailingBlockComment(it.rawExpr).trim();
            String priorComment = it.trailingComment; // may be null

            ParsedAlias pa = parseAlias(commentStripped);
            String exprOnly = pa.exprOnly.trim();
            String alias = pa.alias; // may be null

            // Determine direct column reference
            ColumnRef ref = parseDirectColumnRef(exprOnly);

            ColumnMapping mapping = null;
            if (ref != null) {
                mapping = resolveMapping(aliasTableMap, ref.tableAlias, ref.column);
            } else {
                /* ===== CHANGED START: TOBE 모드 alias 강제치환 대상(컬럼ID alias)만 alias 기반 매핑 시도 ===== */
                if (alias != null && isMappableColumnIdAlias(alias)) {
                    mapping = resolveMapping(aliasTableMap, null, alias);
                    if (mapping == null) mapping = registry.findByColumnOnly(alias);
                } else {
                    mapping = inferMappingFromExpression(aliasTableMap, exprOnly);
                }
                /* ===== CHANGED END ===== */
            }

            // ✅ ASIS 모드에서는 expr를 TOBE로 바꾸지 않는다.
            // ✅ TOBE 모드에서만 expr를 TOBE로 치환한다.
            String exprConverted =
                    (mode == AliasSqlGenerator.Mode.TOBE_SQL)
                            ? convertSqlFragmentToTobe(exprOnly, aliasTableMap, Collections.emptyMap())
                            : exprOnly;

            String outAlias = null;
            String outComment = null;

            if (mode == AliasSqlGenerator.Mode.ASIS_ALIAS_TO_TOBE) {
                /*
                 * ✅ 요청 반영:
                 *   ASIS 모드에서만
                 *     ASIS expr(=원문) AS TOBE_COL /* TOBE 컬럼명 *\/
                 *
                 * - mapping 있으면 alias는 TOBE 컬럼ID로 고정
                 * - mapping 없으면 기존 alias/주석 유지
                 */
                if (mapping != null) {
                    outAlias = mapping.tobeColumnId;
                    outComment = safe(mapping.tobeColumnName);
                } else {
                    outAlias = alias;
                    outComment = priorComment;
                }

                String rendered = render(exprConverted, outAlias, outComment, true);

                // ✅ FIX: SELECT + (space) + first column, next columns on new lines
                if (i == 0) out.append(" ").append(rendered);
                else out.append(",\n    ").append(rendered);

            } else {
                // TOBE_SQL
                if (ref != null) {
                    /* ===== CHANGED START: TOBE 모드에서 '컬럼ID alias' 인 경우만 tobeColumnId로 강제 ===== */
                    boolean hadAliasInOriginal = (alias != null && !alias.isBlank());
                    boolean forceAliasToTobe = hadAliasInOriginal &&
                            (alias.equalsIgnoreCase(ref.column) || isMappableColumnIdAlias(alias));

                    if (mapping != null) {
                        outComment = safe(mapping.tobeColumnName);
                        outAlias = forceAliasToTobe ? mapping.tobeColumnId : null;
                    } else {
                        outComment = priorComment;
                        outAlias = forceAliasToTobe ? alias : null;
                    }

                    String rendered = render(exprConverted, outAlias, outComment, outAlias != null);
                    /* ===== CHANGED END ===== */

                    if (i == 0) out.append(" ").append(rendered);
                    else out.append(",\n    ").append(rendered);

                } else {
                    /* ===== CHANGED START: TOBE 모드에서 함수/표현식 alias가 있으면 "표현식 내부 컬럼ID"로 alias 강제 ===== */
                    boolean hadAliasInOriginal = (alias != null && !alias.isBlank());
                    boolean needInnerAliasOverride = hadAliasInOriginal && !isMappableColumnIdAlias(alias);

                    /* ===== CHANGED START: alias가 함수명/키워드면 무조건 내부 컬럼 alias로 교정 ===== */
                    if (hadAliasInOriginal && isBadExpressionAlias(alias, exprOnly)) {
                        needInnerAliasOverride = true;
                    }
                    /* ===== CHANGED END ===== */

                    ColumnRef innerRef = null;
                    ColumnMapping innerMapping = null;
                    if (needInnerAliasOverride) {
                        innerRef = findFirstColumnRefInExpression(exprOnly);
                        if (innerRef != null) {
                            innerMapping = resolveMapping(aliasTableMap, innerRef.tableAlias, innerRef.column);
                        }
                    }

                    if (innerRef != null) {
                        if (innerMapping != null) {
                            outAlias = innerMapping.tobeColumnId;
                            outComment = safe(innerMapping.tobeColumnName);
                        } else {
                            outAlias = innerRef.column;
                            outComment = priorComment;
                        }
                    } else {
                        boolean forceAliasToTobe = hadAliasInOriginal && isMappableColumnIdAlias(alias);

                        if (mapping != null) {
                            if (!hadAliasInOriginal || forceAliasToTobe) {
                                outAlias = mapping.tobeColumnId;
                                outComment = safe(mapping.tobeColumnName);
                            } else {
                                outAlias = alias;
                                outComment = priorComment;
                            }
                        } else {
                            outAlias = alias;
                            outComment = priorComment;
                        }
                    }

                    String rendered = render(exprConverted, outAlias, outComment, outAlias != null);
                    /* ===== CHANGED END ===== */

                    if (i == 0) out.append(" ").append(rendered);
                    else out.append(",\n    ").append(rendered);
                }
            }
        }

        // ✅ FIX: prevent "*/FROM"
        out.append("\n");
        return out.toString();
    }

    // ------------------------------------------------------------
    // Global conversion (used in TOBE_SQL mode)
    // ------------------------------------------------------------

    public String convertSqlFragmentToTobe(String fragment,
                                           Map<String, String> aliasTableMap,
                                           Map<String, String> paramRenameMap) {
        if (fragment == null || fragment.isEmpty()) return fragment;

        StringBuilder out = new StringBuilder(fragment.length() + 32);

        Scan st = new Scan(fragment);
        while (st.hasNext()) {
            if (st.peekIsLineComment()) {
                out.append(st.readLineComment());
                continue;
            }
            if (st.peekIsBlockComment()) {
                out.append(st.readBlockComment());
                continue;
            }
            if (st.peekIsSingleQuotedString()) {
                out.append(st.readSingleQuotedString());
                continue;
            }
            if (st.peek() == '#') {
                String param = st.readHashParam(); // includes #...#
                out.append(renameParamToken(param, paramRenameMap));
                continue;
            }

            if (isIdentStart(st.peek())) {
                String ident = st.readIdentifier(); // may include dot parts
                String replaced = replaceIdentifierToTobe(ident, aliasTableMap);
                out.append(replaced);
                continue;
            }

            out.append(st.read());
        }

        return out.toString();
    }

    private String renameParamToken(String token, Map<String, String> paramRenameMap) {
        if (token == null) return null;
        if (paramRenameMap == null || paramRenameMap.isEmpty()) return token;

        // token: #NAME#
        if (token.length() < 3) return token;
        String name = token.substring(1, token.length() - 1).trim();
        String u = name.toUpperCase();

        String newName = paramRenameMap.get(u);
        if (newName == null) return token;

        return "#" + newName + "#";
    }

    private String replaceIdentifierToTobe(String ident, Map<String, String> aliasTableMap) {
        if (ident == null) return null;

        String raw = ident;
        String u = raw.toUpperCase();

        String[] parts = u.split("\\.");
        if (parts.length == 1) {
            ColumnMapping m = registry.findByColumnOnly(parts[0]);
            return (m != null) ? m.tobeColumnId : raw;
        }

        String left = parts[0];
        String right = parts[1];

        String table = aliasTableMap.get(left);
        if (table != null) {
            ColumnMapping m = registry.find(table, right);
            if (m == null) m = registry.findByColumnOnly(right);
            return left + "." + ((m != null) ? m.tobeColumnId : right);
        }

        ColumnMapping m = registry.findByColumnOnly(right);
        if (m != null) return left + "." + m.tobeColumnId;

        return raw;
    }

    // ------------------------------------------------------------
    // Param rename inference
    // ------------------------------------------------------------

    public Map<String, String> buildParamRenameMap(String sql, Map<String, String> aliasTableMap) {
        if (sql == null || sql.isEmpty()) return Collections.emptyMap();

        Map<String, String> map = new HashMap<>();

        // A.COL = #PARAM#
        Pattern p = Pattern.compile("(?i)\\b([A-Z0-9_]{1,30})\\s*\\.\\s*([A-Z0-9_]{1,30})\\s*=\\s*#\\s*([A-Z0-9_]{1,60})\\s*#");
        Matcher m = p.matcher(sql.toUpperCase());
        while (m.find()) {
            String a = m.group(1);
            String col = m.group(2);
            String param = m.group(3);

            String table = aliasTableMap.get(a);
            ColumnMapping cm = (table != null) ? registry.find(table, col) : null;
            if (cm == null) cm = registry.findByColumnOnly(col);
            if (cm == null) continue;

            map.put(param, cm.tobeColumnId);
        }

        // COL = #PARAM#
        Pattern p2 = Pattern.compile("(?i)\\b([A-Z0-9_]{1,30})\\s*=\\s*#\\s*([A-Z0-9_]{1,60})\\s*#");
        Matcher m2 = p2.matcher(sql.toUpperCase());
        while (m2.find()) {
            String col = m2.group(1);
            String param = m2.group(2);

            ColumnMapping cm = registry.findByColumnOnly(col);
            if (cm == null) continue;

            map.put(param, cm.tobeColumnId);
        }

        return map;
    }

    // ------------------------------------------------------------
    // DML annotation (INSERT/UPDATE/MERGE)
    // ------------------------------------------------------------

    public String annotateDml(String sql, Map<String, String> aliasTableMap) {
        if (sql == null || sql.isEmpty()) return sql;

        String out = annotateInsertColumnLists(sql, aliasTableMap);
        out = annotateUpdateSetAssignments(out, aliasTableMap);
        return out;
    }

    private String annotateInsertColumnLists(String sql, Map<String, String> aliasTableMap) {
        StringBuilder sb = new StringBuilder(sql.length() + 32);

        Scan st = new Scan(sql);
        while (st.hasNext()) {
            int mark = st.pos;

            if (st.peekIsLineComment()) {
                sb.append(st.readLineComment());
                continue;
            }
            if (st.peekIsBlockComment()) {
                sb.append(st.readBlockComment());
                continue;
            }
            if (st.peekIsSingleQuotedString()) {
                sb.append(st.readSingleQuotedString());
                continue;
            }

            if (st.peekWord("INSERT")) {
                sb.append(st.readWord()); // INSERT
                sb.append(st.readSpaces());

                if (st.peekWord("INTO")) {
                    sb.append(st.readWord()); // INTO
                    sb.append(st.readSpaces());

                    String tableToken = st.readIdentifier();
                    sb.append(tableToken);

                    String tableName = lastPart(tableToken).toUpperCase();

                    int save = st.pos;
                    String spaces = st.readSpaces();
                    if (st.peek() == '(') {
                        sb.append(spaces);
                        String colList = st.readParenBlock(); // includes (...)
                        String annotated = annotateColumnListParen(colList, tableName);
                        sb.append(annotated);
                    } else {
                        st.pos = save;
                    }
                    continue;
                } else {
                    st.pos = mark;
                }
            }

            if (st.peekWord("MERGE")) {
                sb.append(st.read());
                continue;
            }

            sb.append(st.read());
        }

        return sb.toString();
    }

    private String annotateUpdateSetAssignments(String sql, Map<String, String> aliasTableMap) {
        StringBuilder out = new StringBuilder(sql.length() + 64);
        Scan st = new Scan(sql);

        while (st.hasNext()) {
            if (st.peekIsLineComment()) {
                out.append(st.readLineComment());
                continue;
            }
            if (st.peekIsBlockComment()) {
                out.append(st.readBlockComment());
                continue;
            }
            if (st.peekIsSingleQuotedString()) {
                out.append(st.readSingleQuotedString());
                continue;
            }

            if (st.peekWord("SET")) {
                out.append(st.readWord()); // SET
                out.append(st.readSpaces());

                String assignments = st.readUntilSetTerminator();
                out.append(annotateAssignmentsChunk(assignments, aliasTableMap));
                continue;
            }

            out.append(st.read());
        }

        return out.toString();
    }

    private String annotateAssignmentsChunk(String chunk, Map<String, String> aliasTableMap) {
        List<String> parts = splitTopLevelByComma(chunk);
        StringBuilder sb = new StringBuilder(chunk.length() + 32);

        for (int i = 0; i < parts.size(); i++) {
            String one = parts.get(i);

            int eq = indexOfTopLevelEquals(one);
            if (eq > 0) {
                String lhs = one.substring(0, eq).trim();

                ColumnRef ref = parseDirectColumnRef(lhs);
                ColumnMapping cm = null;
                if (ref != null) cm = resolveMapping(aliasTableMap, ref.tableAlias, ref.column);
                else cm = registry.findByColumnOnly(lhs.trim().toUpperCase());

                String annotated = one;
                if (cm != null && !hasBlockComment(one)) {
                    annotated = rtrim(one) + " /* " + cm.tobeColumnName + " */";
                }
                sb.append(annotated);
            } else {
                sb.append(one);
            }

            if (i < parts.size() - 1) sb.append(", ");
        }

        return sb.toString();
    }

    // ------------------------------------------------------------
    // Helpers: select list splitting, parsing, mapping
    // ------------------------------------------------------------

    private static final class SelectItem {
        final String rawExpr;
        final String trailingComment;

        SelectItem(String rawExpr, String trailingComment) {
            this.rawExpr = rawExpr;
            this.trailingComment = trailingComment;
        }
    }

    private static final class ParsedAlias {
        final String exprOnly;
        final String alias;

        ParsedAlias(String exprOnly, String alias) {
            this.exprOnly = exprOnly;
            this.alias = alias;
        }
    }

    private static final class ColumnRef {
        final String tableAlias; // may be null
        final String column;

        ColumnRef(String tableAlias, String column) {
            this.tableAlias = tableAlias;
            this.column = column;
        }
    }

    private List<SelectItem> splitSelectItems(String selectBody) {
        List<String> raw = splitTopLevelByComma(selectBody);
        List<SelectItem> out = new ArrayList<>(raw.size());

        for (String s : raw) {
            String t = s.trim();
            String c = extractTrailingBlockComment(t);
            String expr = (c != null) ? stripTrailingBlockComment(t).trim() : t;
            out.add(new SelectItem(expr, c));
        }
        return out;
    }

    private List<String> splitTopLevelByComma(String s) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isEmpty()) return out;

        StringBuilder cur = new StringBuilder();
        Scan st = new Scan(s);
        int depth = 0;

        while (st.hasNext()) {
            if (st.peekIsLineComment()) {
                cur.append(st.readLineComment());
                continue;
            }
            if (st.peekIsBlockComment()) {
                cur.append(st.readBlockComment());
                continue;
            }
            if (st.peekIsSingleQuotedString()) {
                cur.append(st.readSingleQuotedString());
                continue;
            }

            char ch = st.read();
            if (ch == '(') depth++;
            else if (ch == ')') depth = Math.max(0, depth - 1);

            if (ch == ',' && depth == 0) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }

        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private int indexOfTopLevelEquals(String s) {
        Scan st = new Scan(s);
        int depth = 0;
        int idx = 0;

        while (st.hasNext()) {
            if (st.peekIsLineComment()) { st.readLineComment(); idx = st.pos; continue; }
            if (st.peekIsBlockComment()) { st.readBlockComment(); idx = st.pos; continue; }
            if (st.peekIsSingleQuotedString()) { st.readSingleQuotedString(); idx = st.pos; continue; }

            char ch = st.read();
            if (ch == '(') depth++;
            else if (ch == ')') depth = Math.max(0, depth - 1);
            else if (ch == '=' && depth == 0) return idx;

            idx = st.pos;
        }
        return -1;
    }

    private ParsedAlias parseAlias(String expr) {
        String t = expr.trim();
        if (t.isEmpty()) return new ParsedAlias(expr, null);

        if (t.endsWith(";")) t = t.substring(0, t.length() - 1).trim();

        Matcher m = Pattern.compile("(?i)^(.*)\\bAS\\b\\s+([A-Z0-9_]{1,60})\\s*$").matcher(t);
        if (m.find()) {
            return new ParsedAlias(m.group(1).trim(), m.group(2).trim());
        }

        Matcher m2 = Pattern.compile("^(.*?)(?:\\s+)([A-Z0-9_]{1,60})\\s*$", Pattern.CASE_INSENSITIVE).matcher(t);
        if (m2.find()) {
            String before = m2.group(1).trim();
            String last = m2.group(2).trim();

            if (!before.isEmpty() && !endsWithOperator(before)) {
                return new ParsedAlias(before, last);
            }
        }

        return new ParsedAlias(t, null);
    }

    private boolean endsWithOperator(String s) {
        String t = s.trim();
        if (t.isEmpty()) return false;
        char c = t.charAt(t.length() - 1);
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == '=' || c == '<' || c == '>';
    }

    private ColumnRef parseDirectColumnRef(String expr) {
        String t = expr.trim();
        Matcher m = Pattern.compile("^(?i)([A-Z0-9_]{1,30})\\s*$").matcher(t);
        if (m.find()) {
            return new ColumnRef(null, m.group(1).toUpperCase());
        }
        Matcher m2 = Pattern.compile("^(?i)([A-Z0-9_]{1,30})\\s*\\.\\s*([A-Z0-9_]{1,30})\\s*$").matcher(t);
        if (m2.find()) {
            return new ColumnRef(m2.group(1).toUpperCase(), m2.group(2).toUpperCase());
        }
        return null;
    }

    private ColumnMapping resolveMapping(Map<String, String> aliasTableMap, String tableAlias, String col) {
        if (col == null) return null;
        String c = col.toUpperCase();

        if (tableAlias != null) {
            String table = aliasTableMap.get(tableAlias.toUpperCase());
            if (table != null) {
                ColumnMapping cm = registry.find(table, c);
                if (cm != null) return cm;

                cm = registry.findByTobeOnAsisTable(table, c);
                if (cm != null) return cm;
            }
        }

        ColumnMapping cm = registry.findByColumnOnly(c);
        if (cm != null) return cm;

        return registry.findByTobeColumnOnly(c);
    }

    private ColumnMapping inferMappingFromExpression(Map<String, String> aliasTableMap, String exprOnly) {
        String u = exprOnly.toUpperCase();
        Matcher m = Pattern.compile("(?i)\\b([A-Z0-9_]{1,30})\\s*\\.\\s*([A-Z0-9_]{1,30})\\b").matcher(u);
        if (m.find()) {
            ColumnMapping cm = resolveMapping(aliasTableMap, m.group(1), m.group(2));
            if (cm != null) return cm;
        }
        Matcher m2 = Pattern.compile("(?i)\\b([A-Z0-9_]{1,30})\\b").matcher(u);
        while (m2.find()) {
            String token = m2.group(1);
            if (isSqlKeyword(token)) continue;
            ColumnMapping cm = resolveMapping(aliasTableMap, null, token);
            if (cm != null) return cm;
        }
        return null;
    }

    private boolean isSqlKeyword(String t) {
        String u = t.toUpperCase();
        return u.equals("SELECT") || u.equals("FROM") || u.equals("WHERE") || u.equals("AND") || u.equals("OR")
                || u.equals("CASE") || u.equals("WHEN") || u.equals("THEN") || u.equals("ELSE") || u.equals("END")
                || u.equals("NVL") || u.equals("DECODE") || u.equals("SUM") || u.equals("MAX") || u.equals("MIN")
                || u.equals("COUNT") || u.equals("DISTINCT") || u.equals("AS") || u.equals("IN") || u.equals("IS")
                || u.equals("NULL") || u.equals("NOT") || u.equals("LIKE") || u.equals("ON") || u.equals("JOIN")
                || u.equals("LEFT") || u.equals("RIGHT") || u.equals("INNER") || u.equals("OUTER")
                || u.equals("GROUP") || u.equals("ORDER") || u.equals("BY") || u.equals("HAVING")
                || u.equals("INSERT") || u.equals("UPDATE") || u.equals("DELETE") || u.equals("MERGE")
                || u.equals("INTO") || u.equals("VALUES") || u.equals("SET");
    }

    /* ===== CHANGED START: 컬럼ID alias 판별 ===== */
    private boolean isMappableColumnIdAlias(String alias) {
        if (alias == null || alias.isBlank()) return false;
        String a = alias.trim().toUpperCase();
        return registry.findByColumnOnly(a) != null || registry.findByTobeColumnOnly(a) != null;
    }
    /* ===== CHANGED END ===== */

    /* ===== CHANGED START: 표현식 alias가 함수명/키워드인지 판별 ===== */
    private boolean isBadExpressionAlias(String alias, String exprOnly) {
        if (alias == null || alias.isBlank() || exprOnly == null || exprOnly.isBlank()) return false;

        String a = alias.trim().toUpperCase();

        Matcher fm = Pattern.compile("(?i)^\\s*([A-Z0-9_]{1,60})\\s*\\(").matcher(exprOnly.trim());
        if (fm.find()) {
            String fn = fm.group(1).trim().toUpperCase();
            if (!fn.isEmpty() && a.equals(fn)) return true;
        }

        return isSqlKeyword(a);
    }
    /* ===== CHANGED END ===== */

    /* ===== CHANGED START: 표현식 내부 첫 컬럼 참조 추출 ===== */
    private ColumnRef findFirstColumnRefInExpression(String expr) {
        if (expr == null || expr.isBlank()) return null;

        Scan st = new Scan(expr);
        while (st.hasNext()) {
            if (st.peekIsLineComment()) { st.readLineComment(); continue; }
            if (st.peekIsBlockComment()) { st.readBlockComment(); continue; }
            if (st.peekIsSingleQuotedString()) { st.readSingleQuotedString(); continue; }

            if (isIdentStart(st.peek())) {
                String ident = st.readIdentifier();
                if (ident == null || ident.isBlank()) continue;

                String u = ident.toUpperCase();
                if (isSqlKeyword(u)) continue;

                String[] parts = u.split("\\.");
                if (parts.length >= 2) {
                    String left = parts[0].trim();
                    String right = parts[1].trim();
                    if (!left.isEmpty() && !right.isEmpty() && !isSqlKeyword(right)) {
                        return new ColumnRef(left, right);
                    }
                } else {
                    String one = u.trim();
                    if (!one.isEmpty() && !isSqlKeyword(one)) {
                        return new ColumnRef(null, one);
                    }
                }
                continue;
            }

            st.read();
        }

        return null;
    }
    /* ===== CHANGED END ===== */

    private String render(String expr, String alias, String comment, boolean forceAs) {
        StringBuilder b = new StringBuilder(expr.length() + 32);
        b.append(expr);

        if (alias != null && !alias.isEmpty()) {
            b.append(forceAs ? " AS " : " ");
            b.append(alias);
        }

        if (comment != null && !comment.isEmpty()) {
            b.append(" /* ").append(comment).append(" */");
        }

        return b.toString();
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    // ------------------------------------------------------------
    // Block comment utils
    // ------------------------------------------------------------

    private String extractTrailingBlockComment(String t) {
        if (t == null) return null;
        String s = t.trim();
        if (!s.endsWith("*/")) return null;

        int p = s.lastIndexOf("/*");
        if (p < 0) return null;

        String c = s.substring(p + 2, s.length() - 2).trim();
        return c.isEmpty() ? null : c;
    }

    private String stripTrailingBlockComment(String t) {
        if (t == null) return null;
        String s = t;
        int end = s.lastIndexOf("*/");
        int start = s.lastIndexOf("/*");
        if (start >= 0 && end > start && end == s.length() - 2) {
            return s.substring(0, start).trim();
        }
        return t;
    }

    private boolean hasBlockComment(String s) {
        return s != null && s.contains("/*") && s.contains("*/");
    }

    private String rtrim(String s) {
        int e = s.length();
        while (e > 0 && Character.isWhitespace(s.charAt(e - 1))) e--;
        return s.substring(0, e);
    }

    // ------------------------------------------------------------
    // INSERT column list "(...)" annotation
    // ------------------------------------------------------------

    private String annotateColumnListParen(String parenBlock, String tableName) {
        if (parenBlock == null || parenBlock.isEmpty()) return parenBlock;

        String inside = parenBlock.substring(1, parenBlock.length() - 1);
        List<String> cols = splitTopLevelByComma(inside);

        StringBuilder sb = new StringBuilder(parenBlock.length() + 64);
        sb.append("(");

        for (int i = 0; i < cols.size(); i++) {
            String colRaw = cols.get(i);
            String col = colRaw.trim();

            String colNameOnly = lastPart(col).toUpperCase();
            ColumnMapping cm = registry.find(tableName, colNameOnly);
            if (cm == null) cm = registry.findByColumnOnly(colNameOnly);

            sb.append(colRaw.trim());
            if (cm != null && !hasBlockComment(colRaw)) {
                sb.append(" /* ").append(cm.tobeColumnName).append(" */");
            }

            if (i < cols.size() - 1) sb.append(", ");
        }

        sb.append(")");
        return sb.toString();
    }

    private String lastPart(String ident) {
        if (ident == null) return "";
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

        Scan(String s) { this.s = (s == null) ? "" : s; }

        boolean hasNext() { return pos < s.length(); }

        char peek() { return (pos < s.length()) ? s.charAt(pos) : '\0'; }

        char read() { return (pos < s.length()) ? s.charAt(pos++) : '\0'; }

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

        String readSpaces() {
            int start = pos;
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
            return s.substring(start, pos);
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
            int start = pos;
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
                    if (pos < s.length() && s.charAt(pos) == '\'') { pos++; continue; }
                    break;
                }
            }
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

        String readHashParam() {
            int start = pos;
            pos++; // #
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '#') break;
            }
            return s.substring(start, pos);
        }

        String readParenBlock() {
            if (peek() != '(') return "";
            int start = pos;
            int depth = 0;
            while (pos < s.length()) {
                if (peekIsLineComment()) { readLineComment(); continue; }
                if (peekIsBlockComment()) { readBlockComment(); continue; }
                if (peekIsSingleQuotedString()) { readSingleQuotedString(); continue; }

                char c = read();
                if (c == '(') depth++;
                else if (c == ')') {
                    depth--;
                    if (depth == 0) break;
                }
            }
            return s.substring(start, pos);
        }

        String readUntilSetTerminator() {
            int start = pos;
            int depth = 0;

            while (pos < s.length()) {
                if (peekIsLineComment()) { readLineComment(); continue; }
                if (peekIsBlockComment()) { readBlockComment(); continue; }
                if (peekIsSingleQuotedString()) { readSingleQuotedString(); continue; }

                char c = peek();

                if (depth == 0) {
                    if (peekWord("WHERE") || peekWord("GROUP") || peekWord("ORDER") || peekWord("HAVING")) {
                        break;
                    }
                }

                c = read();
                if (c == '(') depth++;
                else if (c == ')') depth = Math.max(0, depth - 1);
            }

            return s.substring(start, pos);
        }

        private static boolean isWordChar(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
        }
    }

    private static boolean isIdentStart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
    }
}
