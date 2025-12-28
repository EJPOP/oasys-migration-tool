package oasys.migration.alias;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SelectLineTransformer {

    private final ColumnMappingRegistry registry;
    /**
     * 주석 시작 위치(컬럼/표현식 시작점 기준 N칸).
     * - SELECT: "컬럼(표현식) 시작점" 기준으로 N칸에 /* 고정
     * - INSERT/UPDATE/VALUES: "해당 라인에서 컬럼(or 값) 시작점" 기준으로 N칸에 /* 고정
     *
     * 길이가 N칸을 초과하면 최소 1칸 띄우고 바로 뒤에 주석이 붙는다.
     *
     * 기본값: 30
     * - JVM 옵션으로 조정 가능: -Doasys.migration.commentCol=40 (또는 50)
     */
    private static final int COMMENT_COL_FROM_EXPR_START = readCommentCol();

    private static int readCommentCol() {
        // allow both keys for convenience
        String v = System.getProperty("oasys.migration.commentCol");
        if (v == null || v.isBlank()) v = System.getProperty("oasys.migration.comment.col");
        int def = 30;

        if (v == null || v.isBlank()) return def;

        try {
            int n = Integer.parseInt(v.trim());
            // guard rails
            if (n < 10) n = 10;
            if (n > 200) n = 200;
            return n;
        } catch (Exception ignore) {
            return def;
        }
    }

    public SelectLineTransformer(ColumnMappingRegistry registry) {
        this.registry = registry;
    }

    /**
     * Transform SELECT list body (between SELECT and FROM).
     *
     * ✅ Trailing comma -> Leading comma 적용:
     *   SELECT
     *       COL1
     *       , COL2
     */
    public String transformSelectBody(String selectBody,
                                      Map<String, String> aliasTableMap,
                                      AliasSqlGenerator.Mode mode) {

        List<SelectItem> items = splitSelectItems(selectBody);
        if (items.isEmpty()) {
            return selectBody;
        }

        StringBuilder out = new StringBuilder(selectBody.length() + 256);

        for (int i = 0; i < items.size(); i++) {
            SelectItem it = items.get(i);

            String commentStripped = stripTrailingBlockComment(it.rawExpr).trim();
            java.util.List<String> priorComments = it.trailingComments;
            String priorComment = firstNonBlank(priorComments); // may be null

            ParsedAlias pa = parseAlias(commentStripped);
            String exprOnly = pa.exprOnly.trim();
            String alias = pa.alias; // may be null

            ColumnRef ref = parseDirectColumnRef(exprOnly);

            ColumnMapping mapping = null;
            if (ref != null) {
                mapping = resolveMapping(aliasTableMap, ref.tableAlias, ref.column);
            } else {
                if (alias != null && isMappableColumnIdAlias(alias)) {
                    mapping = resolveMapping(aliasTableMap, null, alias);
                    if (mapping == null) mapping = registry.findByColumnOnly(alias);
                } else {
                    mapping = inferMappingFromExpression(aliasTableMap, exprOnly);
                }
            }

            String exprConverted =
                    (mode == AliasSqlGenerator.Mode.TOBE_SQL)
                            ? convertSqlFragmentToTobe(exprOnly, aliasTableMap, Collections.emptyMap())
                            : exprOnly;

            String outAlias = null;
            String outComment = null;
            boolean forceAsForAlias = true;

            if (mode == AliasSqlGenerator.Mode.ASIS_ALIAS_TO_TOBE) {
                if (mapping != null) {
                    outAlias = mapping.tobeColumnId;
                    outComment = safe(mapping.tobeColumnName);
                } else {
                    outAlias = alias;
                    outComment = priorComment;
                }
                forceAsForAlias = true;

            } else {
                if (ref != null) {
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

                    forceAsForAlias = (outAlias != null);

                } else {
                    boolean hadAliasInOriginal = (alias != null && !alias.isBlank());

                    ColumnRef innerRef = null;
                    ColumnMapping innerMapping = null;

                    if (hadAliasInOriginal) {
                        innerRef = findFirstColumnRefInExpression(exprOnly);
                        if (innerRef != null) {
                            innerMapping = resolveMapping(aliasTableMap, innerRef.tableAlias, innerRef.column);
                        }
                    }

                    String innerAlias = null;
                    String innerComment = null;

                    if (innerRef != null) {
                        if (innerMapping != null) {
                            innerAlias = innerMapping.tobeColumnId;
                            innerComment = safe(innerMapping.tobeColumnName);
                        } else {
                            innerAlias = innerRef.column;
                            innerComment = priorComment;
                        }
                    }

                    boolean needInnerAliasOverride =
                            hadAliasInOriginal
                                    && innerAlias != null
                                    && (
                                    isBadExpressionAlias(alias, exprOnly)
                                            || !isMappableColumnIdAlias(alias)
                                            || !alias.equalsIgnoreCase(innerAlias)
                            );

                    if (needInnerAliasOverride) {
                        outAlias = innerAlias;
                        outComment = (innerComment != null && !innerComment.isBlank()) ? innerComment : priorComment;
                    } else {
                        if (mapping != null) {
                            outAlias = mapping.tobeColumnId;
                            outComment = safe(mapping.tobeColumnName);
                        } else {
                            outAlias = alias;
                            outComment = priorComment;
                        }
                    }

                    forceAsForAlias = (outAlias != null);
                }
            }

            String base = buildSelectBase(exprConverted, outAlias, forceAsForAlias);

            java.util.List<String> comments = mergeComments(outComment, priorComments);

            // ✅ leading comma 스타일:
            //  - 첫 항목: "SELECT␠" 뒤라 expr 시작점은 1
            //  - 둘째부터: 라인 시작에 "    , " 가 붙으므로 expr 시작점은 6 (4칸 + ", ")
            int leadingWidth = (i == 0 ? 1 : 6);

            String rendered = renderSelectWithFixedCommentColumn(base, comments, leadingWidth);
            if (i == 0) out.append(" ").append(rendered);
            else out.append("\n    , ").append(rendered);
        }

        out.append("\n");
        return out.toString();
    }

    private String buildSelectBase(String expr, String alias, boolean forceAs) {
        String e = (expr == null) ? "" : expr;
        StringBuilder b = new StringBuilder(e.length() + 32);
        b.append(e);

        if (alias != null && !alias.isEmpty()) {
            b.append(forceAs ? " AS " : " ");
            b.append(alias);
        }
        return b.toString();
    }

    /**
     * SELECT 주석 정렬:
     * - 단일 라인: (leadingWidth = expr 시작점) 기준으로 "exprStart + N"에 /* 고정
     * - 멀티 라인: 마지막 라인의 들여쓰기 기준으로 "indent + N"에 /* 고정
     *
     * 주석이 2개 이상이면:
     * - 첫 주석은 기존 규칙대로 같은 줄에 정렬
     * - 나머지는 다음 줄로 내려서 동일 COMMENT_COL에 정렬
     */
    private String renderSelectWithFixedCommentColumn(String base, java.util.List<String> comments, int leadingWidth) {
        String b = rtrim(base == null ? "" : base);
        if (comments == null || comments.isEmpty()) return b;

        java.util.List<String> cs = new java.util.ArrayList<>();
        for (String c : comments) {
            if (c == null) continue;
            String t = c.trim();
            if (!t.isEmpty()) cs.add(t);
        }
        if (cs.isEmpty()) return b;

        int targetCol;
        int pad;

        if (containsNewline(b)) {
            int indent = lastLineIndentLen(b);
            targetCol = indent + COMMENT_COL_FROM_EXPR_START;
            int curCol = lastLineLen(b);
            pad = targetCol - curCol;
        } else {
            targetCol = Math.max(0, leadingWidth) + COMMENT_COL_FROM_EXPR_START;
            int curAbs = Math.max(0, leadingWidth) + b.length();
            pad = targetCol - curAbs;
        }

        if (pad < 1) pad = 1;

        StringBuilder sb = new StringBuilder(b.length() + 64);
        sb.append(b).append(spaces(pad)).append(wrapBlockComment(cs.get(0)));

        for (int i = 1; i < cs.size(); i++) {
            sb.append("\n").append(spaces(targetCol)).append(wrapBlockComment(cs.get(i)));
        }

        return sb.toString();
    }

    /**
     * DML(INSERT/UPDATE/VALUES) 주석 정렬:

     * - "컬럼/값 시작점" 기준 30칸
     * - leading comma 라인이면 ", " 뒤를 시작점으로 본다.
     */
    private String renderDmlWithFixedCommentColumn(String base, String comment) {
        String b = rtrim(base == null ? "" : base);
        if (comment == null || comment.isEmpty()) return b;

        String c = comment.trim();
        if (c.isEmpty()) return b;

        int exprStart = dmlExprStartCol(b);          // ✅ 콤마 있으면 콤마 뒤
        int targetCol = exprStart + COMMENT_COL_FROM_EXPR_START;
        int curCol = lastLineLen(b);

        int pad = targetCol - curCol;
        if (pad < 1) pad = 1;

        return b + spaces(pad) + "/* " + c + " */";
    }

    // expr start: indent or (indent + ", " 제거)
    private static int dmlExprStartCol(String s) {
        if (s == null) return 0;
        int lineStart = lastLineStartIndex(s);
        int indent = lastLineIndentLen(s);
        int p = lineStart + indent;

        if (p < s.length() && s.charAt(p) == ',') {
            p++;
            while (p < s.length()) {
                char ch = s.charAt(p);
                if (ch == ' ' || ch == '\t') { p++; continue; }
                break;
            }
            return p - lineStart;
        }
        return indent;
    }

    private static int lastLineStartIndex(String s) {
        int p1 = s.lastIndexOf('\n');
        int p2 = s.lastIndexOf('\r');
        int p = Math.max(p1, p2);
        return (p < 0) ? 0 : (p + 1);
    }

    private static String spaces(int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(' ');
        return sb.toString();
    }

    private static boolean containsNewline(String s) {
        if (s == null || s.isEmpty()) return false;
        return s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
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
            if (st.peekIsLineComment()) { out.append(st.readLineComment()); continue; }
            if (st.peekIsBlockComment()) { out.append(st.readBlockComment()); continue; }
            if (st.peekIsSingleQuotedString()) { out.append(st.readSingleQuotedString()); continue; }
            if (st.peekIsMyBatisParam()) { out.append(st.readMyBatisParam()); continue; }

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
    // DML annotation + leading comma (INSERT/VALUES/UPDATE/merge)
    // ------------------------------------------------------------

    public String annotateDml(String sql, Map<String, String> aliasTableMap) {
        if (sql == null || sql.isEmpty()) return sql;

        String out = annotateInsertColumnLists(sql, aliasTableMap);      // ✅ INSERT / MERGE INSERT 컬럼 리스트
        out = formatValuesParenBlocksLeadingComma(out);                 // ✅ VALUES (...) 리스트
        out = annotateUpdateSetAssignments(out, aliasTableMap);         // ✅ UPDATE SET / MERGE UPDATE SET
        return out;
    }

    /**
     * INSERT/ MERGE INSERT 컬럼 리스트:
     * - INSERT INTO T ( ... )
     * - MERGE INTO T ... WHEN NOT MATCHED THEN INSERT ( ... )
     */
    private String annotateInsertColumnLists(String sql, Map<String, String> aliasTableMap) {
        StringBuilder sb = new StringBuilder(sql.length() + 64);

        Scan st = new Scan(sql);
        String mergeIntoTable = null;

        while (st.hasNext()) {
            if (st.peekIsLineComment()) { sb.append(st.readLineComment()); continue; }
            if (st.peekIsBlockComment()) { sb.append(st.readBlockComment()); continue; }
            if (st.peekIsSingleQuotedString()) { sb.append(st.readSingleQuotedString()); continue; }
            if (st.peekIsMyBatisParam()) { sb.append(st.readMyBatisParam()); continue; }

            // MERGE INTO <table> 캡쳐
            if (st.peekWord("MERGE")) {
                sb.append(st.readWord()); // MERGE
                sb.append(st.readSpaces());

                if (st.peekWord("INTO")) {
                    sb.append(st.readWord()); // INTO
                    sb.append(st.readSpaces());

                    String tableToken = st.readIdentifier();
                    sb.append(tableToken);

                    mergeIntoTable = lastPart(tableToken).toUpperCase(Locale.ROOT);
                    continue;
                }
                continue;
            }

            if (st.peekWord("INSERT")) {
                sb.append(st.readWord()); // INSERT
                sb.append(st.readSpaces());

                String tableName;

                if (st.peekWord("INTO")) {
                    sb.append(st.readWord()); // INTO
                    sb.append(st.readSpaces());

                    String tableToken = st.readIdentifier();
                    sb.append(tableToken);

                    tableName = lastPart(tableToken).toUpperCase(Locale.ROOT);

                    sb.append(st.readSpaces());
                } else {
                    // MERGE INSERT: INSERT ( ... )
                    tableName = mergeIntoTable;
                    sb.append(st.readSpaces());
                }

                if (st.peek() == '(') {
                    String colList = st.readParenBlock();
                    String annotated = annotateColumnListParenLeadingComma(colList, tableName);
                    sb.append(annotated);
                    continue;
                }
                continue;
            }

            sb.append(st.read());
        }

        return sb.toString();
    }

    /**
     * VALUES (...) 리스트를 찾아 trailing -> leading comma로 변환
     * (DELETE는 VALUES가 없으니 자연스럽게 제외됨)
     */
    private String formatValuesParenBlocksLeadingComma(String sql) {
        StringBuilder out = new StringBuilder(sql.length() + 64);
        Scan st = new Scan(sql);

        while (st.hasNext()) {
            if (st.peekIsLineComment()) { out.append(st.readLineComment()); continue; }
            if (st.peekIsBlockComment()) { out.append(st.readBlockComment()); continue; }
            if (st.peekIsSingleQuotedString()) { out.append(st.readSingleQuotedString()); continue; }
            if (st.peekIsMyBatisParam()) { out.append(st.readMyBatisParam()); continue; }

            if (st.peekWord("VALUES")) {
                out.append(st.readWord()); // VALUES
                out.append(st.readSpaces());

                if (st.peek() == '(') {
                    String paren = st.readParenBlock();
                    out.append(formatParenListLeadingComma(paren));
                    continue;
                }
                continue;
            }

            out.append(st.read());
        }

        return out.toString();
    }

    private String annotateUpdateSetAssignments(String sql, Map<String, String> aliasTableMap) {
        StringBuilder out = new StringBuilder(sql.length() + 64);
        Scan st = new Scan(sql);

        // ✅ UPDATE / MERGE UPDATE SET에서, 별칭 없는 컬럼도 정확히 매핑하기 위한 기본 테이블 컨텍스트
        // - 일반 UPDATE: UPDATE <table> ... SET
        // - MERGE UPDATE: MERGE INTO <table> ... WHEN MATCHED THEN UPDATE SET
        String defaultUpdateTable = null; // ASIS table id (UPPER)
        String mergeIntoTable = null;     // ASIS table id (UPPER)

        while (st.hasNext()) {
            if (st.peekIsLineComment()) { out.append(st.readLineComment()); continue; }
            if (st.peekIsBlockComment()) { out.append(st.readBlockComment()); continue; }
            if (st.peekIsSingleQuotedString()) { out.append(st.readSingleQuotedString()); continue; }
            if (st.peekIsMyBatisParam()) { out.append(st.readMyBatisParam()); continue; }

            // MERGE INTO <table> 캡쳐 (MERGE UPDATE의 기본 테이블)
            if (st.peekWord("MERGE")) {
                out.append(st.readWord()); // MERGE
                out.append(st.readSpaces());

                if (st.peekWord("INTO")) {
                    out.append(st.readWord()); // INTO
                    out.append(st.readSpaces());

                    // 힌트/주석이 끼어도 테이블까지 전진
                    consumeHintsAndSpaces(st, out);

                    if (isIdentStart(st.peek())) {
                        String tableToken = st.readIdentifier();
                        out.append(tableToken);
                        mergeIntoTable = lastPart(tableToken).toUpperCase(Locale.ROOT);
                    }
                    continue;
                }
                continue;
            }

            // UPDATE <table> 캡쳐 (일반 UPDATE의 기본 테이블)
            // - MERGE UPDATE는 "UPDATE SET" 형태이므로 테이블이 없다 => mergeIntoTable을 기본 테이블로 사용
            if (st.peekWord("UPDATE")) {
                out.append(st.readWord()); // UPDATE
                out.append(st.readSpaces());

                consumeHintsAndSpaces(st, out);

                if (st.peekWord("SET")) {
                    // MERGE ... WHEN MATCHED THEN UPDATE SET
                    if (mergeIntoTable != null && !mergeIntoTable.isBlank()) {
                        defaultUpdateTable = mergeIntoTable;
                    }
                    continue;
                }

                if (isIdentStart(st.peek())) {
                    String tableToken = st.readIdentifier();
                    out.append(tableToken);
                    defaultUpdateTable = lastPart(tableToken).toUpperCase(Locale.ROOT);
                    continue;
                }

                continue;
            }

            if (st.peekWord("SET")) {
                out.append(st.readWord()); // SET
                out.append(st.readSpaces());

                String assignments = st.readUntilSetTerminator(); // ✅ MERGE WHEN 경계 포함
                out.append(annotateAssignmentsChunkLeadingComma(assignments, aliasTableMap, defaultUpdateTable));
                continue;
            }

            out.append(st.read());
        }

        return out.toString();
    }

    /**
     * UPDATE SET / MERGE UPDATE SET:
     * - splitTopLevelByComma로 나눈 뒤, 각 항목을 leading comma로 재조립
     * - 마지막 assignment 뒤에 WHERE/WHEN 등이 붙는 케이스(SYSDATEWHERE 등) 방지 위해 trailing whitespace 보존/보강
     */
    private String annotateAssignmentsChunkLeadingComma(String chunk,
                                                        Map<String, String> aliasTableMap,
                                                        String defaultUpdateTable) {
        if (chunk == null || chunk.isEmpty()) return chunk;

        // ✅ 마지막 assignment 뒤 trailing whitespace(특히 newline)를 보존한다.
        String tailWs = trailingWhitespace(chunk);
        String body = chunk.substring(0, chunk.length() - tailWs.length());

        // WHERE/WHEN 경계는 다음 라인으로 내리는 것이 안전 (요구사항)
        if (tailWs.isEmpty() || !containsNewline(tailWs)) {
            tailWs = "\n";
        }

        List<String> parts = splitTopLevelByComma(body);
        StringBuilder sb = new StringBuilder(chunk.length() + 64);

        String defaultTable = (defaultUpdateTable == null) ? null : defaultUpdateTable.trim().toUpperCase(Locale.ROOT);

        for (int i = 0; i < parts.size(); i++) {
            String raw = parts.get(i);

            // ✅ 기존 leading comma가 있었더라도 안전하게 제거 후 다시 적용
            String base = rtrim(stripLeadingComma(raw));
            boolean newlineStyle = startsWithNewlineWhitespace(raw);

            if (i > 0) {
                base = prefixLeadingComma(base, newlineStyle);
            }

            int eq = indexOfTopLevelEquals(base);
            ColumnMapping cm = null;

            if (eq > 0) {
                String lhsRaw = base.substring(0, eq);
                String lhs = stripLeadingDecorationsForLhs(lhsRaw);

                ColumnRef ref = parseDirectColumnRef(lhs);
                if (ref != null) {
                    cm = resolveMappingForUpdateAssignment(aliasTableMap, defaultTable, ref);
                } else {
                    // 방어: 주석/잡토큰이 남아 direct-ref로 못 잡는 경우
                    String key = lastPart(lhs).trim().toUpperCase(Locale.ROOT);

                    if (defaultTable != null && !defaultTable.isBlank()) {
                        cm = registry.find(defaultTable, key);
                        if (cm == null) cm = registry.findByTobeOnAsisTable(defaultTable, key);
                    }

                    if (cm == null) {
                        cm = registry.findByColumnOnly(key);
                        if (cm == null) cm = registry.findByTobeColumnOnly(key);
                    }
                }
            }

            // ✅ "컬럼 앞"에 달린 설명 주석(/* ... */)은 허용하고,
            //    assignment 끝에 이미 주석이 있으면 중복 주석을 피한다.
            if (cm != null && extractTrailingBlockComment(base) == null) {
                String c = pickDmlComment(cm);
                if (!c.isBlank()) {
                    base = renderDmlWithFixedCommentColumn(base, c);
                }
            }

            sb.append(base);
        }

        sb.append(tailWs);
        return sb.toString();
    }

    private static String stripLeadingDecorationsForLhs(String s) {
        if (s == null || s.isEmpty()) return "";

        int i = 0;
        while (i < s.length()) {
            // whitespace
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;

            // leading comma
            if (i < s.length() && s.charAt(i) == ',') {
                i++;
                continue;
            }

            // block comment
            if (i + 1 < s.length() && s.charAt(i) == '/' && s.charAt(i + 1) == '*') {
                int j = i + 2;
                while (j + 1 < s.length()) {
                    if (s.charAt(j) == '*' && s.charAt(j + 1) == '/') { j += 2; break; }
                    j++;
                }
                i = j;
                continue;
            }

            // line comment
            if (i + 1 < s.length() && s.charAt(i) == '-' && s.charAt(i + 1) == '-') {
                int j = i + 2;
                while (j < s.length() && s.charAt(j) != '\n') j++;
                i = j;
                continue;
            }

            break;
        }

        return s.substring(i).trim();
    }

    private ColumnMapping resolveMappingForUpdateAssignment(Map<String, String> aliasTableMap,
                                                            String defaultTable,
                                                            ColumnRef ref) {
        if (ref == null) return null;

        // alias.col
        if (ref.tableAlias != null && !ref.tableAlias.isBlank()) {
            return resolveMapping(aliasTableMap, ref.tableAlias, ref.column);
        }

        String col = (ref.column == null) ? null : ref.column.trim().toUpperCase(Locale.ROOT);
        if (col == null || col.isBlank()) return null;

        // unqualified col: prefer UPDATE target table
        if (defaultTable != null && !defaultTable.isBlank()) {
            ColumnMapping cm = registry.find(defaultTable, col);
            if (cm != null) return cm;

            cm = registry.findByTobeOnAsisTable(defaultTable, col);
            if (cm != null) return cm;
        }

        return resolveMapping(aliasTableMap, null, col);
    }

    private String pickDmlComment(ColumnMapping cm) {
        if (cm == null) return "";
        String name = safe(cm.tobeColumnName);
        if (!name.isBlank()) return name;

        // 컬럼명이 비어있으면 최소한 TOBE 컬럼ID라도 붙인다
        return safe(cm.tobeColumnId);
    }

    private static void consumeHintsAndSpaces(Scan st, StringBuilder out) {
        boolean progressed;
        do {
            progressed = false;

            if (st.peekIsLineComment()) {
                out.append(st.readLineComment());
                progressed = true;
            } else if (st.peekIsBlockComment()) {
                out.append(st.readBlockComment());
                progressed = true;
            }

            String ws = st.readSpaces();
            if (!ws.isEmpty()) {
                out.append(ws);
                progressed = true;
            }

        } while (progressed);
    }

    // ------------------------------------------------------------
    // Helpers: select list splitting, parsing, mapping
    // ------------------------------------------------------------

    private static final class SelectItem {
        final String rawExpr;
        // comment bodies (without /* */)
        final java.util.List<String> trailingComments;

        SelectItem(String rawExpr, java.util.List<String> trailingComments) {
            this.rawExpr = rawExpr;
            if (trailingComments == null || trailingComments.isEmpty()) {
                this.trailingComments = java.util.Collections.emptyList();
            } else {
                java.util.List<String> tmp = new java.util.ArrayList<>();
                for (String c : trailingComments) {
                    if (c == null) continue;
                    String t = c.trim();
                    if (!t.isEmpty()) tmp.add(t);
                }
                this.trailingComments = tmp.isEmpty()
                        ? java.util.Collections.emptyList()
                        : java.util.Collections.unmodifiableList(tmp);
            }
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
        List<String> rawItems = splitTopLevelByComma(selectBody);
        if (rawItems.isEmpty()) return java.util.Collections.emptyList();

        // 1) 콤마 바로 위(이전 세그먼트 끝)에 있는 단독 주석 라인을 다음 세그먼트로 이동
        java.util.List<java.util.List<String>> carry = new java.util.ArrayList<>(rawItems.size());
        for (int i = 0; i < rawItems.size(); i++) carry.add(new java.util.ArrayList<>());

        for (int i = 0; i < rawItems.size() - 1; i++) {
            CommentExtractResult tr = extractTrailingStandaloneComments(rawItems.get(i));
            rawItems.set(i, tr.sql);
            if (!tr.comments.isEmpty()) {
                carry.get(i + 1).addAll(tr.comments);
            }
        }

        // 2) 콤마 뒤(세그먼트 시작)에 있는 단독 주석 라인은 같은 세그먼트의 뒤쪽 주석으로 이동
        for (int i = 0; i < rawItems.size(); i++) {
            CommentExtractResult lead = extractLeadingStandaloneComments(rawItems.get(i));
            rawItems.set(i, lead.sql);
            if (!lead.comments.isEmpty()) {
                carry.get(i).addAll(lead.comments);
            }
        }

        // 3) 각 세그먼트의 끝에 붙은 inline 주석( /* ... */ , -- ... )도 trailingComments로 수집
        java.util.List<SelectItem> out = new java.util.ArrayList<>();
        for (int i = 0; i < rawItems.size(); i++) {
            String seg = rawItems.get(i);

            CommentExtractResult tail = extractTrailingInlineComments(seg);
            String expr = rtrim((tail.sql == null) ? "" : tail.sql.trim());

            java.util.List<String> comments = new java.util.ArrayList<>();
            comments.addAll(carry.get(i));
            comments.addAll(tail.comments);

            out.add(new SelectItem(expr, comments));
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
            if (st.peekIsLineComment()) { cur.append(st.readLineComment()); continue; }
            if (st.peekIsBlockComment()) { cur.append(st.readBlockComment()); continue; }
            if (st.peekIsSingleQuotedString()) { cur.append(st.readSingleQuotedString()); continue; }
            if (st.peekIsMyBatisParam()) { cur.append(st.readMyBatisParam()); continue; }

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
            if (st.peekIsMyBatisParam()) { st.readMyBatisParam(); idx = st.pos; continue; }

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

        Matcher m = Pattern.compile("(?is)^(.*)\\bAS\\b\\s+([A-Z0-9_]{1,60})\\s*$").matcher(t);
        if (m.find()) {
            return new ParsedAlias(m.group(1).trim(), m.group(2).trim());
        }

        Matcher m2 = Pattern.compile("(?is)^(.*?)(?:\\s+)([A-Z0-9_]{1,60})\\s*$").matcher(t);
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
            if (isSqlKeywordForExprScan(token)) continue;
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

    private boolean isSqlKeywordForExprScan(String t) {
        if (t == null) return true;
        return isSqlKeyword(t);
    }

    private boolean isMappableColumnIdAlias(String alias) {
        if (alias == null || alias.isBlank()) return false;
        String a = alias.trim().toUpperCase();
        return registry.findByColumnOnly(a) != null || registry.findByTobeColumnOnly(a) != null;
    }

    private boolean isBadExpressionAlias(String alias, String exprOnly) {
        if (alias == null || alias.isBlank() || exprOnly == null || exprOnly.isBlank()) return false;

        String a = alias.trim().toUpperCase();

        Matcher fm = Pattern.compile("(?i)^\\s*([A-Z0-9_]{1,60})\\s*\\(").matcher(exprOnly.trim());
        if (fm.find()) {
            String fn = fm.group(1).trim().toUpperCase();
            if (!fn.isEmpty() && a.equals(fn)) return true;
        }

        return isSqlKeywordForExprScan(a);
    }

    private ColumnRef findFirstColumnRefInExpression(String expr) {
        if (expr == null || expr.isBlank()) return null;

        Scan st = new Scan(expr);
        while (st.hasNext()) {
            if (st.peekIsLineComment()) { st.readLineComment(); continue; }
            if (st.peekIsBlockComment()) { st.readBlockComment(); continue; }
            if (st.peekIsSingleQuotedString()) { st.readSingleQuotedString(); continue; }
            if (st.peekIsMyBatisParam()) { st.readMyBatisParam(); continue; }

            if (isIdentStart(st.peek())) {
                String ident = st.readIdentifier();
                if (ident == null || ident.isBlank()) continue;

                String u = ident.toUpperCase(Locale.ROOT);

                if (isSqlKeywordForExprScan(u)) continue;

                st.readSpaces();
                if (st.peek() == '(') {
                    continue;
                }

                String[] parts = u.split("\\.");
                if (parts.length >= 2) {
                    String left = parts[0].trim();
                    String right = parts[1].trim();
                    if (!left.isEmpty() && !right.isEmpty() && !isSqlKeywordForExprScan(right)) {
                        return new ColumnRef(left, right);
                    }
                } else {
                    String one = u.trim();
                    if (!one.isEmpty() && !isSqlKeywordForExprScan(one)) {
                        return new ColumnRef(null, one);
                    }
                }
                continue;
            }

            st.read();
        }

        return null;
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }



    private static String firstNonBlank(java.util.List<String> comments) {
        if (comments == null) return null;
        for (String c : comments) {
            if (c == null) continue;
            String t = c.trim();
            if (!t.isEmpty()) return t;
        }
        return null;
    }

    private static java.util.List<String> mergeComments(String primary, java.util.List<String> others) {
        java.util.List<String> out = new java.util.ArrayList<>();

        if (primary != null && !primary.trim().isEmpty()) {
            out.add(primary.trim());
        }

        if (others != null) {
            for (String c : others) {
                if (c == null) continue;
                String t = c.trim();
                if (t.isEmpty()) continue;
                boolean dup = false;
                for (String e : out) {
                    if (e.equalsIgnoreCase(t)) { dup = true; break; }
                }
                if (!dup) out.add(t);
            }
        }

        return out;
    }

    private static String wrapBlockComment(String body) {
        if (body == null) return "/* */";
        String b = body.trim();
        if (b.isEmpty()) return "/* */";

        // keep Oracle hint style if present: /*+ ... */
        if (b.startsWith("+")) {
            return "/*" + b + " */";
        }
        return "/* " + b + " */";
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
        if (s == null) return "";
        int e = s.length();
        while (e > 0 && Character.isWhitespace(s.charAt(e - 1))) e--;
        return s.substring(0, e);
    }

    // ------------------------------------------------------------
    // Standalone comment relocation within SELECT item lists
    //
    // Pattern we want to fix:
    //   <item-A>
    //       /* comment for item-B */
    //   , <item-B>
    //
    // The comma-splitter attaches the comment to item-A (because it appears before the comma).
    // We move such standalone comment-lines to item-B, and we also move leading standalone
    // comments that appear right after the comma to the *end* of the same item.
    // ------------------------------------------------------------

    private static final class CommentExtractResult {
        final String sql;
        final List<String> comments; // comment bodies

        CommentExtractResult(String sql, List<String> comments) {
            this.sql = (sql == null) ? "" : sql;
            this.comments = (comments == null) ? Collections.emptyList() : comments;
        }
    }

    /**
     * Extract standalone comment lines at the *start* of a comma-split segment.
     * - Only treats a block comment as standalone if it ends the current line
     *   (i.e., followed by a newline before any other token).
     * - Line comments are always standalone.
     */
    private CommentExtractResult extractLeadingStandaloneComments(String segment) {
        if (segment == null || segment.isEmpty()) return new CommentExtractResult("", new ArrayList<>());

        String s = segment;
        int i = 0;
        int n = s.length();
        List<String> comments = new ArrayList<>();

        while (i < n) {
            int wsStart = i;
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;

            if (i + 1 < n && s.charAt(i) == '/' && s.charAt(i + 1) == '*') {
                int end = s.indexOf("*/", i + 2);
                if (end < 0) break;
                end += 2;

                // standalone only if a newline appears before any non-ws token after the comment
                int j = end;
                boolean sawNl = false;
                while (j < n) {
                    char ch = s.charAt(j);
                    if (ch == '\n' || ch == '\r') { sawNl = true; break; }
                    if (!Character.isWhitespace(ch)) { sawNl = false; break; }
                    j++;
                }
                if (!sawNl && j < n && !Character.isWhitespace(s.charAt(j))) {
                    // inline block comment; stop
                    i = wsStart;
                    break;
                }

                comments.add(blockCommentBody(s.substring(i, end)));
                i = end;
                continue;
            }

            if (i + 1 < n && s.charAt(i) == '-' && s.charAt(i + 1) == '-') {
                int end = i + 2;
                while (end < n && s.charAt(end) != '\n' && s.charAt(end) != '\r') end++;
                comments.add(lineCommentBody(s.substring(i, end)));
                i = end;
                continue;
            }

            i = wsStart;
            break;
        }

        String rest = (i < n) ? s.substring(i) : "";
        return new CommentExtractResult(rest, comments);
    }

    /**
     * Extract standalone comment lines at the *end* of a comma-split segment,
     * where the comment begins on its own line (only whitespace after the last newline).
     */
    private CommentExtractResult extractTrailingStandaloneComments(String segment) {
        if (segment == null || segment.isEmpty()) return new CommentExtractResult("", new ArrayList<>());

        String s = segment;
        List<String> comments = new ArrayList<>();

        while (true) {
            String t = rtrim(s);
            if (t.isEmpty()) { s = t; break; }

            if (t.endsWith("*/")) {
                int start = t.lastIndexOf("/*");
                if (start >= 0) {
                    String before = t.substring(0, start);
                    if (onlyWhitespaceSinceLastLineBreak(before)) {
                        comments.add(0, blockCommentBody(t.substring(start)));
                        s = before;
                        continue;
                    }
                }
            }

            int dash = t.lastIndexOf("--");
            if (dash >= 0) {
                String before = t.substring(0, dash);
                if (onlyWhitespaceSinceLastLineBreak(before)) {
                    comments.add(0, lineCommentBody(t.substring(dash)));
                    s = before;
                    continue;
                }
            }

            break;
        }

        return new CommentExtractResult(s, comments);
    }

    /**
     * Extract trailing inline comments (block or line) at end of segment.
     * Collects multiple trailing comments (e.g. "... /*a* / /*b* /").
     */
    private CommentExtractResult extractTrailingInlineComments(String segment) {
        if (segment == null || segment.isEmpty()) return new CommentExtractResult("", new ArrayList<>());

        String s = segment;
        List<String> comments = new ArrayList<>();

        while (true) {
            String t = rtrim(s);
            if (t.isEmpty()) { s = t; break; }

            if (t.endsWith("*/")) {
                int start = t.lastIndexOf("/*");
                if (start >= 0) {
                    comments.add(0, blockCommentBody(t.substring(start)));
                    s = t.substring(0, start);
                    continue;
                }
            }

            int dash = t.lastIndexOf("--");
            if (dash >= 0) {
                int nl = Math.max(t.lastIndexOf('\n'), t.lastIndexOf('\r'));
                if (nl < dash) {
                    comments.add(0, lineCommentBody(t.substring(dash)));
                    s = t.substring(0, dash);
                    continue;
                }
            }

            break;
        }

        return new CommentExtractResult(s, comments);
    }

    private static boolean onlyWhitespaceSinceLastLineBreak(String s) {
        if (s == null || s.isEmpty()) return true;
        int lastNl = Math.max(s.lastIndexOf('\n'), s.lastIndexOf('\r'));
        String tail = (lastNl >= 0) ? s.substring(lastNl + 1) : s;
        return tail.trim().isEmpty();
    }

    private static String blockCommentBody(String commentToken) {
        if (commentToken == null) return "";
        String c = commentToken.trim();
        if (!c.startsWith("/*") || !c.endsWith("*/")) return c;
        return c.substring(2, c.length() - 2).trim();
    }

    private static String lineCommentBody(String lineCommentToken) {
        if (lineCommentToken == null) return "";
        String c = lineCommentToken.trim();
        if (c.startsWith("--")) c = c.substring(2);
        return c.trim();
    }


    // ------------------------------------------------------------
    // INSERT column list "(...)" annotation + leading comma
    // ------------------------------------------------------------

    private String annotateColumnListParenLeadingComma(String parenBlock, String tableName) {
        if (parenBlock == null || parenBlock.isEmpty()) return parenBlock;
        if (parenBlock.length() < 2) return parenBlock;

        String insideAll = parenBlock.substring(1, parenBlock.length() - 1);

        // 닫는 괄호를 새 줄로 내리기 위해 trailing whitespace 분리
        String tailWs = trailingWhitespace(insideAll);
        String inside = insideAll.substring(0, insideAll.length() - tailWs.length());

        List<String> cols = splitTopLevelByComma(inside);
        String tn = (tableName == null) ? "" : tableName.trim().toUpperCase(Locale.ROOT);

        boolean multiline = containsNewline(insideAll);
        if (multiline && !containsNewline(tailWs)) {
            tailWs = "\n" + guessIndent(cols);
        }

        StringBuilder sb = new StringBuilder(parenBlock.length() + 128);
        sb.append("(");

        for (int i = 0; i < cols.size(); i++) {
            String raw = cols.get(i);

            String base = rtrim(stripLeadingComma(raw));
            boolean newlineStyle = startsWithNewlineWhitespace(raw);

            if (i > 0) {
                base = prefixLeadingComma(base, newlineStyle);
            }

            String colNameOnly = lastPart(stripLeadingComma(raw).trim()).toUpperCase(Locale.ROOT);

            ColumnMapping cm = registry.find(tn, colNameOnly);
            if (cm == null) cm = registry.findByTobeOnAsisTable(tn, colNameOnly);
            if (cm == null) cm = registry.findByColumnOnly(colNameOnly);
            if (cm == null) cm = registry.findByTobeColumnOnly(colNameOnly);

            if (cm != null && !hasBlockComment(base)) {
                String c = safe(cm.tobeColumnName);
                if (!c.isBlank()) {
                    base = renderDmlWithFixedCommentColumn(base, c);
                }
            }

            sb.append(base);
        }

        sb.append(tailWs);
        sb.append(")");
        return sb.toString();
    }

    /**
     * VALUES (...) 리스트를 trailing -> leading comma로 변환 (주석 추가 X)
     */
    private String formatParenListLeadingComma(String parenBlock) {
        if (parenBlock == null || parenBlock.isEmpty()) return parenBlock;
        if (parenBlock.length() < 2) return parenBlock;

        String insideAll = parenBlock.substring(1, parenBlock.length() - 1);
        String tailWs = trailingWhitespace(insideAll);
        String inside = insideAll.substring(0, insideAll.length() - tailWs.length());

        List<String> vals = splitTopLevelByComma(inside);

        boolean multiline = containsNewline(insideAll);
        if (multiline && !containsNewline(tailWs)) {
            tailWs = "\n" + guessIndent(vals);
        }

        StringBuilder sb = new StringBuilder(parenBlock.length() + 128);
        sb.append("(");

        for (int i = 0; i < vals.size(); i++) {
            String raw = vals.get(i);

            String base = rtrim(stripLeadingComma(raw));
            boolean newlineStyle = startsWithNewlineWhitespace(raw);

            if (i > 0) {
                base = prefixLeadingComma(base, newlineStyle);
            }

            sb.append(base);
        }

        sb.append(tailWs);
        sb.append(")");
        return sb.toString();
    }

    private String lastPart(String ident) {
        if (ident == null) return "";
        String t = ident.trim();
        int p = t.lastIndexOf('.');
        return (p >= 0) ? t.substring(p + 1) : t;
    }

    private static int lastLineLen(String s) {
        if (s == null) return 0;
        int n = s.length();
        int p1 = s.lastIndexOf('\n');
        int p2 = s.lastIndexOf('\r');
        int p = Math.max(p1, p2);
        return (p < 0) ? n : (n - (p + 1));
    }

    private static int lastLineIndentLen(String s) {
        if (s == null || s.isEmpty()) return 0;
        int p1 = s.lastIndexOf('\n');
        int p2 = s.lastIndexOf('\r');
        int p = Math.max(p1, p2);
        int start = (p < 0) ? 0 : (p + 1);

        int i = start;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t') {
                i++;
                continue;
            }
            break;
        }
        return i - start;
    }

    private static boolean startsWithNewlineWhitespace(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r') return true;
            if (!Character.isWhitespace(c)) return false;
        }
        return false;
    }

    // ------------------------------------------------------------
    // leading comma helpers
    // ------------------------------------------------------------

    private static String stripLeadingComma(String s) {
        if (s == null || s.isEmpty()) return "";
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        if (i < s.length() && s.charAt(i) == ',') {
            int j = i + 1;
            while (j < s.length()) {
                char ch = s.charAt(j);
                if (ch == ' ' || ch == '\t') { j++; continue; }
                break;
            }
            return s.substring(0, i) + s.substring(j);
        }
        return s;
    }

    private static String prefixLeadingComma(String base, boolean newlineStyle) {
        String b = (base == null) ? "" : base;
        b = stripLeadingComma(b);

        if (newlineStyle) {
            int i = 0;
            while (i < b.length() && Character.isWhitespace(b.charAt(i))) i++;
            String ws = b.substring(0, i);
            String rest = ltrim(b.substring(i));
            return ws + ", " + rest;
        } else {
            return ", " + ltrim(b);
        }
    }

    private static String ltrim(String s) {
        if (s == null || s.isEmpty()) return "";
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return s.substring(i);
    }

    private static String trailingWhitespace(String s) {
        if (s == null || s.isEmpty()) return "";
        int e = s.length();
        while (e > 0 && Character.isWhitespace(s.charAt(e - 1))) e--;
        return s.substring(e);
    }

    private static String guessIndent(List<String> tokens) {
        if (tokens == null) return "";
        for (String t : tokens) {
            if (t == null) continue;
            int i = 0;
            while (i < t.length() && Character.isWhitespace(t.charAt(i))) i++;
            if (i == 0) continue;

            String lead = t.substring(0, i);
            int nl1 = lead.lastIndexOf('\n');
            int nl2 = lead.lastIndexOf('\r');
            int nl = Math.max(nl1, nl2);
            return (nl >= 0) ? lead.substring(nl + 1) : lead;
        }
        return "";
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

        // ✅ MERGE 경계: "WHEN MATCHED" / "WHEN NOT"만 종료로 인정 (CASE WHEN 보호)
        boolean peekMergeWhenBoundary() {
            int save = pos;
            if (!peekWord("WHEN")) return false;
            readWord(); // WHEN
            readSpaces();
            boolean ok = peekWord("MATCHED") || peekWord("NOT");
            pos = save;
            return ok;
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

        boolean peekIsMyBatisParam() {
            if (pos >= s.length()) return false;
            char c = s.charAt(pos);
            if (c != '#' && c != '$') return false;

            int q = pos + 1;
            while (q < s.length() && Character.isWhitespace(s.charAt(q))) q++;
            return q < s.length() && s.charAt(q) == '{';
        }

        String readMyBatisParam() {
            int start = pos;
            pos++; // # or $
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
            if (pos < s.length() && s.charAt(pos) == '{') pos++;

            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '}') break;
            }
            return s.substring(start, pos);
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
                if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '$') {
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
                if (peekIsMyBatisParam()) { readMyBatisParam(); continue; }

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
                if (peekIsMyBatisParam()) { readMyBatisParam(); continue; }

                if (depth == 0) {
                    if (peekWord("WHERE") || peekWord("GROUP") || peekWord("ORDER") || peekWord("HAVING")) break;
                    if (peekWord("DELETE") || peekMergeWhenBoundary()) break; // ✅ MERGE 경계
                }

                char c = read();
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
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }
}
