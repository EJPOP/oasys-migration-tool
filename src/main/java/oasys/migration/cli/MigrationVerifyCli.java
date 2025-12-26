package oasys.migration.cli;

import oasys.migration.callchain.ServiceSqlCall;
import oasys.migration.callchain.ServiceSqlXrefLoader;
import oasys.migration.sql.SqlStatement;
import oasys.migration.sql.SqlStatementRegistry;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MigrationVerifyCli {

    private static volatile String CURRENT_KEY = "";
    private static final AtomicInteger CURRENT_INDEX = new AtomicInteger(0);

    public static void main(String[] args) {

        long t0 = System.nanoTime();
        Map<String, String> argv = parseArgs(args);

        String callchainCsv = argv.getOrDefault("csv", "mapping/service_sql_xref.csv");
        Path tobeDir = Path.of(argv.getOrDefault("tobeDir", "output/tobe-sql"));
        Path resultXlsx = Path.of(argv.getOrDefault("result", "output/meta-compare.xlsx"));

        int max = parseInt(argv.get("max"), -1);
        int logEvery = parseInt(argv.get("logEvery"), 200);
        boolean failFast = parseBoolean(argv.get("failFast"), false);

        System.out.println("==================================================");
        System.out.println("[START] Migration meta compare (NO EXEC)");
        System.out.println("[CONF] csv      = " + callchainCsv);
        System.out.println("[CONF] tobeDir  = " + tobeDir.toAbsolutePath());
        System.out.println("[CONF] result   = " + resultXlsx.toAbsolutePath());
        System.out.println("[CONF] max      = " + max);
        System.out.println("[CONF] logEvery = " + logEvery);
        System.out.println("[CONF] failFast = " + failFast);
        System.out.println("==================================================");

        if (resultXlsx.getParent() != null) mkdirs(resultXlsx.getParent());

        // SQL registry (ASIS sql_text fallback용)
        long tSqlIdx0 = System.nanoTime();
        SqlStatementRegistry sqlRegistry = new SqlStatementRegistry();
        sqlRegistry.initialize(); // 내부에서 size 출력
        System.out.println("[STEP1] SqlStatementRegistry.initialize() done. elapsed=" + ms(tSqlIdx0) + "ms");

        // xref csv
        long tCsv0 = System.nanoTime();
        System.out.println("[STEP2] loading xref csv...");
        ServiceSqlXrefLoader xrefLoader = new ServiceSqlXrefLoader();
        List<ServiceSqlCall> calls = xrefLoader.load(callchainCsv);
        System.out.println("[STEP2] xref csv loaded. size=" + calls.size() + ", elapsed=" + ms(tCsv0) + "ms");

        if (max > 0 && calls.size() > max) {
            calls = calls.subList(0, max);
            System.out.println("[STEP2] apply max => truncated to " + calls.size());
        }

        startHeartbeat(calls.size());

        long tLoop0 = System.nanoTime();
        int total = calls.size();
        System.out.println("[STEP3] extracting meta start. total=" + total);

        try (SXSSFWorkbook wb = new SXSSFWorkbook(200)) {
            wb.setCompressTempFiles(true);

            // sheets
            Sheet shAsisSel = wb.createSheet("ASIS_SELECT");
            Sheet shTobeSel = wb.createSheet("TOBE_SELECT");
            Sheet shCmpSel  = wb.createSheet("CMP_SELECT");

            Sheet shAsisDml = wb.createSheet("ASIS_DML_PARAM");
            Sheet shTobeDml = wb.createSheet("TOBE_DML_PARAM");
            Sheet shCmpDml  = wb.createSheet("CMP_DML_PARAM");

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle bodyStyle = createBodyStyle(wb);

            int rAsisSel = 0, rTobeSel = 0, rCmpSel = 0, rAsisDml = 0, rTobeDml = 0, rCmpDml = 0;

            // headers
            rAsisSel = writeSelectHeader(shAsisSel, rAsisSel, headerStyle);
            rTobeSel = writeSelectHeader(shTobeSel, rTobeSel, headerStyle);
            rCmpSel  = writeSelectCompareHeader(shCmpSel, rCmpSel, headerStyle);

            rAsisDml = writeDmlHeader(shAsisDml, rAsisDml, headerStyle);
            rTobeDml = writeDmlHeader(shTobeDml, rTobeDml, headerStyle);
            rCmpDml  = writeDmlCompareHeader(shCmpDml, rCmpDml, headerStyle);

            int success = 0;
            int skip = 0;

            for (int i = 0; i < total; i++) {
                CURRENT_INDEX.set(i + 1);

                ServiceSqlCall call = calls.get(i);
                String svc = safe(call.getServiceClass());
                String ns  = safe(call.getMapperNamespace());
                String id  = safe(call.getSqlId());

                CURRENT_KEY = svc + " | " + ns + "." + id;

                // ASIS sql_text
                String asisSql = call.getSqlText();
                if (asisSql == null || asisSql.isBlank()) {
                    SqlStatement stmt = null;
                    try { stmt = sqlRegistry.get(ns, id); } catch (Exception ignored) {}
                    if (stmt != null) asisSql = stmt.getSqlText();
                }

                // TOBE sql_text (generated file)
                String tobeSql = readTobeSqlFile(tobeDir, svc, ns, id);

                if ((asisSql == null || asisSql.isBlank()) && (tobeSql == null || tobeSql.isBlank())) {
                    skip++;
                    if ((i + 1) % logEvery == 0 || (i + 1) == total) {
                        logProgress(i + 1, total, success, skip, tLoop0, CURRENT_KEY);
                    }
                    continue;
                }

                try {
                    SqlMeta asisMeta = parseSqlMeta(asisSql);
                    SqlMeta tobeMeta = parseSqlMeta(tobeSql);

                    // SELECT
                    if (asisMeta.type.equals("SELECT")) {
                        rAsisSel = writeSelectRows(shAsisSel, rAsisSel, bodyStyle, svc, ns, id, asisMeta.selectOutputs);
                    }
                    if (tobeMeta.type.equals("SELECT")) {
                        rTobeSel = writeSelectRows(shTobeSel, rTobeSel, bodyStyle, svc, ns, id, tobeMeta.selectOutputs);
                    }
                    if (asisMeta.type.equals("SELECT") || tobeMeta.type.equals("SELECT")) {
                        rCmpSel = writeSelectCompareRows(shCmpSel, rCmpSel, bodyStyle, svc, ns, id,
                                asisMeta.selectOutputs, tobeMeta.selectOutputs);
                    }

                    // DML
                    if (isDmlType(asisMeta.type)) {
                        rAsisDml = writeDmlRows(shAsisDml, rAsisDml, bodyStyle, svc, ns, id, asisMeta.type, asisMeta.dmlParams);
                    }
                    if (isDmlType(tobeMeta.type)) {
                        rTobeDml = writeDmlRows(shTobeDml, rTobeDml, bodyStyle, svc, ns, id, tobeMeta.type, tobeMeta.dmlParams);
                    }
                    if (isDmlType(asisMeta.type) || isDmlType(tobeMeta.type)) {
                        rCmpDml = writeDmlCompareRows(shCmpDml, rCmpDml, bodyStyle, svc, ns, id,
                                asisMeta.type, asisMeta.dmlParams, tobeMeta.type, tobeMeta.dmlParams);
                    }

                    success++;
                } catch (Exception e) {
                    skip++;
                    System.out.println("[ERROR] meta extract failed: " + CURRENT_KEY);
                    System.out.println("        ex=" + e.getClass().getName() + ": " + safe(e.getMessage()));
                    e.printStackTrace(System.out);
                    if (failFast) break;
                }

                if ((i + 1) % logEvery == 0 || (i + 1) == total) {
                    logProgress(i + 1, total, success, skip, tLoop0, CURRENT_KEY);
                }
            }

            // column widths (fixed)
            setSelectSheetWidths(shAsisSel);
            setSelectSheetWidths(shTobeSel);
            setSelectCompareSheetWidths(shCmpSel);

            setDmlSheetWidths(shAsisDml);
            setDmlSheetWidths(shTobeDml);
            setDmlCompareSheetWidths(shCmpDml);

            // write xlsx
            long tWrite0 = System.nanoTime();
            try (OutputStream os = Files.newOutputStream(resultXlsx,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                wb.write(os);
            }
            wb.dispose();
            System.out.println("[STEP4] result xlsx written. elapsed=" + ms(tWrite0) + "ms");

        } catch (Exception e) {
            throw new RuntimeException("Failed to write meta-compare xlsx: " + resultXlsx, e);
        }

        System.out.println("==================================================");
        System.out.println("[DONE] totalElapsed=" + ms(t0) + "ms");
        System.out.println("==================================================");
    }

    // ----------------------------------------------------------------
    // Reading TOBE SQL file (matches AliasSqlFileWriter path rule)
    // ----------------------------------------------------------------

    private static String readTobeSqlFile(Path tobeDir, String serviceClass, String namespace, String sqlId) {
        if (tobeDir == null) return null;
        try {
            Path p = tobeDir
                    .resolve(toSafePath(serviceClass))
                    .resolve(toSafePath(namespace))
                    .resolve(toSafePath(sqlId) + ".sql");
            if (!Files.exists(p)) return null;
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static String toSafePath(String s) {
        if (s == null) return "_";
        String t = s.trim();
        if (t.isEmpty()) return "_";
        t = t.replace('.', '/').replace(':', '/');
        t = t.replaceAll("[\\\\?%\\*:|\"<>]", "_");
        return t;
    }

    // ----------------------------------------------------------------
    // Meta extraction
    // ----------------------------------------------------------------

    private static final class SqlMeta {
        final String type; // SELECT / INSERT / UPDATE / DELETE / MERGE / OTHER / EMPTY
        final List<SelectOutput> selectOutputs;
        final List<DmlParam> dmlParams;

        SqlMeta(String type, List<SelectOutput> selectOutputs, List<DmlParam> dmlParams) {
            this.type = type;
            this.selectOutputs = (selectOutputs == null) ? Collections.emptyList() : selectOutputs;
            this.dmlParams = (dmlParams == null) ? Collections.emptyList() : dmlParams;
        }
    }

    private static final class SelectOutput {
        final int seq;
        final String outputName;     // e.g. COL_ID
        final String lowerCamel;     // e.g. colId
        final String expr;
        final String trailingComment;

        SelectOutput(int seq, String outputName, String lowerCamel, String expr, String trailingComment) {
            this.seq = seq;
            this.outputName = outputName;
            this.lowerCamel = lowerCamel;
            this.expr = expr;
            this.trailingComment = trailingComment;
        }
    }

    private static final class DmlParam {
        final int seq;
        final String rawToken;       // e.g. #AAA_BBB#
        final String nameUpper;      // e.g. AAA_BBB
        final String lowerCamel;     // e.g. aaaBbb

        DmlParam(int seq, String rawToken, String nameUpper, String lowerCamel) {
            this.seq = seq;
            this.rawToken = rawToken;
            this.nameUpper = nameUpper;
            this.lowerCamel = lowerCamel;
        }
    }

    private static SqlMeta parseSqlMeta(String sql) {
        if (sql == null || sql.isBlank()) {
            return new SqlMeta("EMPTY", Collections.emptyList(), Collections.emptyList());
        }

        if (isLeadingKeyword(sql, "SELECT")) {
            List<SelectOutput> outs = extractSelectOutputs(sql);
            return new SqlMeta("SELECT", outs, Collections.emptyList());
        }

        if (isLeadingKeyword(sql, "INSERT")) {
            return new SqlMeta("INSERT", Collections.emptyList(), extractDmlParams(sql));
        }
        if (isLeadingKeyword(sql, "UPDATE")) {
            return new SqlMeta("UPDATE", Collections.emptyList(), extractDmlParams(sql));
        }
        if (isLeadingKeyword(sql, "DELETE")) {
            return new SqlMeta("DELETE", Collections.emptyList(), extractDmlParams(sql));
        }
        if (isLeadingKeyword(sql, "MERGE")) {
            return new SqlMeta("MERGE", Collections.emptyList(), extractDmlParams(sql));
        }

        return new SqlMeta("OTHER", Collections.emptyList(), Collections.emptyList());
    }

    private static boolean isDmlType(String type) {
        return "INSERT".equals(type) || "UPDATE".equals(type) || "DELETE".equals(type) || "MERGE".equals(type);
    }

    // SELECT outputs
    private static List<SelectOutput> extractSelectOutputs(String sql) {
        String selectBody = extractTopLevelSelectBody(sql);
        if (selectBody == null || selectBody.isBlank()) return Collections.emptyList();

        List<String> items = splitTopLevelByComma(selectBody);
        List<SelectOutput> out = new ArrayList<>(Math.max(4, items.size()));

        int seq = 0;
        for (String raw : items) {
            String item = (raw == null) ? "" : raw.trim();
            if (item.isEmpty()) continue;

            String trailingComment = extractTrailingBlockComment(item);
            String exprNoComment = (trailingComment != null) ? stripTrailingBlockComment(item).trim() : item;

            ParsedAlias pa = parseAlias(exprNoComment);
            String expr = pa.exprOnly.trim();
            String alias = pa.alias;

            String inferredFromExpr = firstColumnIdInExpression(expr);
            String outputName;

            if (alias != null && !alias.isBlank()) {
                // alias가 함수명/의미없는 값이면 expr 내부 COL로 강제 (요구사항 케이스 대응)
                if (inferredFromExpr != null && !looksLikeColumnId(alias)) {
                    outputName = inferredFromExpr;
                } else if (inferredFromExpr != null && looksLikeColumnId(inferredFromExpr) && looksLikeColumnId(alias)
                        && !alias.equalsIgnoreCase(inferredFromExpr)) {
                    outputName = inferredFromExpr;
                } else {
                    outputName = alias;
                }
            } else {
                if (inferredFromExpr != null) outputName = inferredFromExpr;
                else outputName = lastIdentifierPart(expr);
            }

            if (outputName == null || outputName.isBlank()) continue;

            String lc = toLowerCamel(outputName);
            out.add(new SelectOutput(++seq, outputName, lc, expr, trailingComment));
        }

        return out;
    }

    private static String extractTopLevelSelectBody(String sql) {
        String s = stripLeadingSpaceAndComments(sql);
        int n = s.length();

        int selectPos = indexOfKeywordTopLevel(s, 0, "SELECT");
        if (selectPos < 0) return null;

        int afterSelect = selectPos + 6;
        while (afterSelect < n && Character.isWhitespace(s.charAt(afterSelect))) afterSelect++;

        int fromPos = indexOfKeywordTopLevel(s, afterSelect, "FROM");
        if (fromPos < 0) return null;

        return s.substring(afterSelect, fromPos).trim();
    }

    private static boolean isLeadingKeyword(String sql, String kw) {
        String s = stripLeadingSpaceAndComments(sql);
        if (s.isEmpty()) return false;

        int n = kw.length();
        if (s.length() < n) return false;

        for (int i = 0; i < n; i++) {
            if (Character.toUpperCase(s.charAt(i)) != Character.toUpperCase(kw.charAt(i))) return false;
        }

        if (s.length() > n) {
            char next = s.charAt(n);
            if (isWordChar(next)) return false;
        }
        return true;
    }

    private static String stripLeadingSpaceAndComments(String sql) {
        if (sql == null) return "";
        int i = 0;
        int n = sql.length();

        while (i < n) {
            while (i < n && Character.isWhitespace(sql.charAt(i))) i++;

            if (i + 1 < n && sql.charAt(i) == '-' && sql.charAt(i + 1) == '-') {
                i += 2;
                while (i < n && sql.charAt(i) != '\n') i++;
                continue;
            }

            if (i + 1 < n && sql.charAt(i) == '/' && sql.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n) {
                    if (sql.charAt(i) == '*' && sql.charAt(i + 1) == '/') { i += 2; break; }
                    i++;
                }
                continue;
            }

            break;
        }

        return (i >= n) ? "" : sql.substring(i);
    }

    private static int indexOfKeywordTopLevel(String s, int start, String kw) {
        int n = s.length();
        int depth = 0;
        boolean inStr = false;

        int i = Math.max(0, start);
        while (i < n) {
            char c = s.charAt(i);

            if (!inStr) {
                if (i + 1 < n && s.charAt(i) == '-' && s.charAt(i + 1) == '-') {
                    i += 2;
                    while (i < n && s.charAt(i) != '\n') i++;
                    continue;
                }
                if (i + 1 < n && s.charAt(i) == '/' && s.charAt(i + 1) == '*') {
                    i += 2;
                    while (i + 1 < n) {
                        if (s.charAt(i) == '*' && s.charAt(i + 1) == '/') { i += 2; break; }
                        i++;
                    }
                    continue;
                }
            }

            if (c == '\'') {
                if (inStr) {
                    if (i + 1 < n && s.charAt(i + 1) == '\'') { i += 2; continue; }
                    inStr = false;
                    i++;
                    continue;
                } else {
                    inStr = true;
                    i++;
                    continue;
                }
            }

            if (inStr) { i++; continue; }

            if (c == '(') { depth++; i++; continue; }
            if (c == ')') { depth = Math.max(0, depth - 1); i++; continue; }

            if (depth == 0) {
                if (matchesKeywordAt(s, i, kw)) return i;
            }

            i++;
        }

        return -1;
    }

    private static boolean matchesKeywordAt(String s, int i, String kw) {
        int n = s.length();
        int k = kw.length();
        if (i + k > n) return false;

        if (i > 0 && isWordChar(s.charAt(i - 1))) return false;

        for (int j = 0; j < k; j++) {
            if (Character.toUpperCase(s.charAt(i + j)) != Character.toUpperCase(kw.charAt(j))) return false;
        }

        if (i + k < n && isWordChar(s.charAt(i + k))) return false;
        return true;
    }

    private static List<String> splitTopLevelByComma(String s) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isEmpty()) return out;

        StringBuilder cur = new StringBuilder();
        int depth = 0;
        boolean inStr = false;

        int i = 0;
        int n = s.length();
        while (i < n) {
            if (!inStr) {
                if (i + 1 < n && s.charAt(i) == '-' && s.charAt(i + 1) == '-') {
                    int start = i;
                    i += 2;
                    while (i < n && s.charAt(i) != '\n') i++;
                    cur.append(s, start, i);
                    continue;
                }
                if (i + 1 < n && s.charAt(i) == '/' && s.charAt(i + 1) == '*') {
                    int start = i;
                    i += 2;
                    while (i + 1 < n) {
                        if (s.charAt(i) == '*' && s.charAt(i + 1) == '/') { i += 2; break; }
                        i++;
                    }
                    cur.append(s, start, i);
                    continue;
                }
            }

            char c = s.charAt(i);

            if (c == '\'') {
                if (inStr) {
                    if (i + 1 < n && s.charAt(i + 1) == '\'') {
                        cur.append("''");
                        i += 2;
                        continue;
                    }
                    inStr = false;
                    cur.append(c);
                    i++;
                    continue;
                } else {
                    inStr = true;
                    cur.append(c);
                    i++;
                    continue;
                }
            }

            if (!inStr) {
                if (c == '(') depth++;
                else if (c == ')') depth = Math.max(0, depth - 1);

                if (c == ',' && depth == 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                    i++;
                    continue;
                }
            }

            cur.append(c);
            i++;
        }

        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private static final class ParsedAlias {
        final String exprOnly;
        final String alias;

        ParsedAlias(String exprOnly, String alias) {
            this.exprOnly = exprOnly;
            this.alias = alias;
        }
    }

    private static ParsedAlias parseAlias(String expr) {
        String t = (expr == null) ? "" : expr.trim();
        if (t.isEmpty()) return new ParsedAlias("", null);

        if (t.endsWith(";")) t = t.substring(0, t.length() - 1).trim();

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?i)^(.*)\\bAS\\b\\s+([A-Z0-9_]{1,120})\\s*$")
                .matcher(t);
        if (m.find()) {
            return new ParsedAlias(m.group(1).trim(), m.group(2).trim());
        }

        java.util.regex.Matcher m2 = java.util.regex.Pattern
                .compile("^(.*?)(?:\\s+)([A-Z0-9_]{1,120})\\s*$", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(t);
        if (m2.find()) {
            String before = m2.group(1).trim();
            String last = m2.group(2).trim();

            if (!before.isEmpty() && !endsWithOperator(before)) {
                return new ParsedAlias(before, last);
            }
        }

        return new ParsedAlias(t, null);
    }

    private static boolean endsWithOperator(String s) {
        String t = (s == null) ? "" : s.trim();
        if (t.isEmpty()) return false;
        char c = t.charAt(t.length() - 1);
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == '=' || c == '<' || c == '>';
    }

    private static String extractTrailingBlockComment(String t) {
        if (t == null) return null;
        String s = t.trim();
        if (!s.endsWith("*/")) return null;

        int p = s.lastIndexOf("/*");
        if (p < 0) return null;

        String c = s.substring(p + 2, s.length() - 2).trim();
        return c.isEmpty() ? null : c;
    }

    private static String stripTrailingBlockComment(String t) {
        if (t == null) return null;
        String s = t;
        int end = s.lastIndexOf("*/");
        int start = s.lastIndexOf("/*");
        if (start >= 0 && end > start && end == s.length() - 2) {
            return s.substring(0, start).trim();
        }
        return t;
    }

    private static boolean looksLikeColumnId(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.isEmpty()) return false;
        boolean hasUnderscore = t.indexOf('_') >= 0;
        boolean allUpperOrDigitOrUnderscore = true;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (!(Character.isUpperCase(c) || Character.isDigit(c) || c == '_')) {
                allUpperOrDigitOrUnderscore = false;
                break;
            }
        }
        return hasUnderscore && allUpperOrDigitOrUnderscore;
    }

    private static String firstColumnIdInExpression(String expr) {
        if (expr == null) return null;
        String u = expr.toUpperCase(Locale.ROOT);

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?i)\\b([A-Z0-9_]{1,30})\\s*\\.\\s*([A-Z0-9_]{1,60})\\b")
                .matcher(u);
        if (m.find()) {
            return m.group(2);
        }

        java.util.regex.Matcher m2 = java.util.regex.Pattern
                .compile("(?i)\\b([A-Z0-9_]{2,80})\\b")
                .matcher(u);
        while (m2.find()) {
            String tok = m2.group(1);
            if (isSqlKeyword(tok)) continue;
            if (looksLikeColumnId(tok)) return tok;
        }
        return null;
    }

    private static String lastIdentifierPart(String expr) {
        if (expr == null) return null;
        String t = expr.trim();
        if (t.isEmpty()) return null;

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?i)\\b([A-Z0-9_]{1,30})\\s*\\.\\s*([A-Z0-9_]{1,60})\\b(?!.*\\b([A-Z0-9_]{1,30})\\s*\\.\\s*([A-Z0-9_]{1,60})\\b)")
                .matcher(t);
        if (m.find()) return m.group(2);

        java.util.regex.Matcher m2 = java.util.regex.Pattern
                .compile("(?i)\\b([A-Z0-9_]{2,80})\\b(?!.*\\b([A-Z0-9_]{2,80})\\b)")
                .matcher(t);
        if (m2.find()) return m2.group(1).toUpperCase(Locale.ROOT);

        return null;
    }

    private static boolean isSqlKeyword(String t) {
        if (t == null) return true;
        String u = t.toUpperCase(Locale.ROOT);
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

    // DML params
    private static List<DmlParam> extractDmlParams(String sql) {
        LinkedHashSet<String> tokens = extractHashParams(sql);
        if (tokens.isEmpty()) return Collections.emptyList();

        List<DmlParam> out = new ArrayList<>(tokens.size());
        int seq = 0;
        for (String tok : tokens) {
            String nameUpper = normalizeParamName(tok);
            if (nameUpper == null || nameUpper.isBlank()) continue;
            String lc = toLowerCamel(nameUpper);
            out.add(new DmlParam(++seq, tok, nameUpper, lc));
        }
        return out;
    }

    private static LinkedHashSet<String> extractHashParams(String sql) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (sql == null || sql.isEmpty()) return out;

        int n = sql.length();
        int i = 0;
        boolean inStr = false;

        while (i < n) {
            char c = sql.charAt(i);

            if (!inStr) {
                if (i + 1 < n && sql.charAt(i) == '-' && sql.charAt(i + 1) == '-') {
                    i += 2;
                    while (i < n && sql.charAt(i) != '\n') i++;
                    continue;
                }
                if (i + 1 < n && sql.charAt(i) == '/' && sql.charAt(i + 1) == '*') {
                    i += 2;
                    while (i + 1 < n) {
                        if (sql.charAt(i) == '*' && sql.charAt(i + 1) == '/') { i += 2; break; }
                        i++;
                    }
                    continue;
                }
            }

            if (c == '\'') {
                if (inStr) {
                    if (i + 1 < n && sql.charAt(i + 1) == '\'') { i += 2; continue; }
                    inStr = false;
                    i++;
                    continue;
                } else {
                    inStr = true;
                    i++;
                    continue;
                }
            }

            if (inStr) { i++; continue; }

            if (c == '#') {
                int start = i;
                i++;
                while (i < n && sql.charAt(i) != '#') i++;
                if (i < n && sql.charAt(i) == '#') {
                    String token = sql.substring(start, i + 1);
                    out.add(token);
                    i++;
                    continue;
                }
                break;
            }

            i++;
        }

        return out;
    }

    private static String normalizeParamName(String hashToken) {
        if (hashToken == null) return null;
        String t = hashToken.trim();
        if (t.length() < 3) return null;
        if (!t.startsWith("#") || !t.endsWith("#")) return null;

        String inner = t.substring(1, t.length() - 1).trim();
        if (inner.isEmpty()) return null;

        int dot = inner.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < inner.length()) inner = inner.substring(dot + 1);

        StringBuilder sb = new StringBuilder(inner.length());
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') sb.append(c);
            else break;
        }

        String name = sb.toString();
        return name.isEmpty() ? null : name.toUpperCase(Locale.ROOT);
    }

    private static String toLowerCamel(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.isEmpty()) return "";

        t = t.replace('-', '_');

        String[] parts = t.split("_+");
        StringBuilder out = new StringBuilder(t.length());

        boolean first = true;
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;

            String lower = p.toLowerCase(Locale.ROOT);

            if (first) {
                out.append(lower);
                first = false;
            } else {
                out.append(Character.toUpperCase(lower.charAt(0)));
                if (lower.length() > 1) out.append(lower.substring(1));
            }
        }

        if (out.length() == 0) return t;
        return out.toString();
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
    }

    // ----------------------------------------------------------------
    // XLSX writing helpers (streaming)
    // ----------------------------------------------------------------

    private static int writeSelectHeader(Sheet sh, int rowIdx, CellStyle hs) {
        Row r = sh.createRow(rowIdx++);
        int c = 0;
        createCell(r, c++, "SERVICE_CLASS", hs);
        createCell(r, c++, "NAMESPACE", hs);
        createCell(r, c++, "SQL_ID", hs);
        createCell(r, c++, "SEQ", hs);
        createCell(r, c++, "OUTPUT_NAME", hs);
        createCell(r, c++, "LOWER_CAMEL", hs);
        createCell(r, c++, "EXPR", hs);
        createCell(r, c++, "TRAILING_COMMENT", hs);
        return rowIdx;
    }

    private static int writeSelectCompareHeader(Sheet sh, int rowIdx, CellStyle hs) {
        Row r = sh.createRow(rowIdx++);
        int c = 0;
        createCell(r, c++, "SERVICE_CLASS", hs);
        createCell(r, c++, "NAMESPACE", hs);
        createCell(r, c++, "SQL_ID", hs);
        createCell(r, c++, "SEQ", hs);
        createCell(r, c++, "ASIS_OUTPUT", hs);
        createCell(r, c++, "ASIS_LOWER_CAMEL", hs);
        createCell(r, c++, "TOBE_OUTPUT", hs);
        createCell(r, c++, "TOBE_LOWER_CAMEL", hs);
        createCell(r, c++, "STATUS", hs);
        return rowIdx;
    }

    private static int writeDmlHeader(Sheet sh, int rowIdx, CellStyle hs) {
        Row r = sh.createRow(rowIdx++);
        int c = 0;
        createCell(r, c++, "SERVICE_CLASS", hs);
        createCell(r, c++, "NAMESPACE", hs);
        createCell(r, c++, "SQL_ID", hs);
        createCell(r, c++, "SEQ", hs);
        createCell(r, c++, "DML_TYPE", hs);
        createCell(r, c++, "PARAM_NAME", hs);
        createCell(r, c++, "LOWER_CAMEL", hs);
        createCell(r, c++, "RAW_TOKEN", hs);
        return rowIdx;
    }

    private static int writeDmlCompareHeader(Sheet sh, int rowIdx, CellStyle hs) {
        Row r = sh.createRow(rowIdx++);
        int c = 0;
        createCell(r, c++, "SERVICE_CLASS", hs);
        createCell(r, c++, "NAMESPACE", hs);
        createCell(r, c++, "SQL_ID", hs);
        createCell(r, c++, "SEQ", hs);
        createCell(r, c++, "ASIS_DML_TYPE", hs);
        createCell(r, c++, "ASIS_PARAM", hs);
        createCell(r, c++, "ASIS_LOWER_CAMEL", hs);
        createCell(r, c++, "TOBE_DML_TYPE", hs);
        createCell(r, c++, "TOBE_PARAM", hs);
        createCell(r, c++, "TOBE_LOWER_CAMEL", hs);
        createCell(r, c++, "STATUS", hs);
        return rowIdx;
    }

    private static int writeSelectRows(Sheet sh, int rowIdx, CellStyle bs,
                                       String svc, String ns, String id, List<SelectOutput> outs) {
        for (SelectOutput o : outs) {
            Row r = sh.createRow(rowIdx++);
            int c = 0;
            createCell(r, c++, svc, bs);
            createCell(r, c++, ns, bs);
            createCell(r, c++, id, bs);
            createCell(r, c++, String.valueOf(o.seq), bs);
            createCell(r, c++, safe(o.outputName), bs);
            createCell(r, c++, safe(o.lowerCamel), bs);
            createCell(r, c++, safe(o.expr), bs);
            createCell(r, c++, safe(o.trailingComment), bs);
        }
        return rowIdx;
    }

    private static int writeSelectCompareRows(Sheet sh, int rowIdx, CellStyle bs,
                                              String svc, String ns, String id,
                                              List<SelectOutput> asis, List<SelectOutput> tobe) {
        int max = Math.max(asis.size(), tobe.size());
        for (int i = 0; i < max; i++) {
            SelectOutput a = (i < asis.size()) ? asis.get(i) : null;
            SelectOutput t = (i < tobe.size()) ? tobe.get(i) : null;

            String aOut = (a == null) ? "" : safe(a.outputName);
            String aLc  = (a == null) ? "" : safe(a.lowerCamel);
            String tOut = (t == null) ? "" : safe(t.outputName);
            String tLc  = (t == null) ? "" : safe(t.lowerCamel);

            String status;
            if (a == null && t != null) status = "MISSING_ASIS";
            else if (a != null && t == null) status = "MISSING_TOBE";
            else if (aLc.equals(tLc)) status = "MATCH";
            else status = "DIFF";

            Row r = sh.createRow(rowIdx++);
            int c = 0;
            createCell(r, c++, svc, bs);
            createCell(r, c++, ns, bs);
            createCell(r, c++, id, bs);
            createCell(r, c++, String.valueOf(i + 1), bs);
            createCell(r, c++, aOut, bs);
            createCell(r, c++, aLc, bs);
            createCell(r, c++, tOut, bs);
            createCell(r, c++, tLc, bs);
            createCell(r, c++, status, bs);
        }
        return rowIdx;
    }

    private static int writeDmlRows(Sheet sh, int rowIdx, CellStyle bs,
                                    String svc, String ns, String id, String dmlType, List<DmlParam> params) {
        for (DmlParam p : params) {
            Row r = sh.createRow(rowIdx++);
            int c = 0;
            createCell(r, c++, svc, bs);
            createCell(r, c++, ns, bs);
            createCell(r, c++, id, bs);
            createCell(r, c++, String.valueOf(p.seq), bs);
            createCell(r, c++, dmlType, bs);
            createCell(r, c++, safe(p.nameUpper), bs);
            createCell(r, c++, safe(p.lowerCamel), bs);
            createCell(r, c++, safe(p.rawToken), bs);
        }
        return rowIdx;
    }

    private static int writeDmlCompareRows(Sheet sh, int rowIdx, CellStyle bs,
                                           String svc, String ns, String id,
                                           String asisType, List<DmlParam> asis,
                                           String tobeType, List<DmlParam> tobe) {
        int max = Math.max(asis.size(), tobe.size());
        for (int i = 0; i < max; i++) {
            DmlParam a = (i < asis.size()) ? asis.get(i) : null;
            DmlParam t = (i < tobe.size()) ? tobe.get(i) : null;

            String aNm = (a == null) ? "" : safe(a.nameUpper);
            String aLc = (a == null) ? "" : safe(a.lowerCamel);
            String tNm = (t == null) ? "" : safe(t.nameUpper);
            String tLc = (t == null) ? "" : safe(t.lowerCamel);

            String status;
            if (a == null && t != null) status = "MISSING_ASIS";
            else if (a != null && t == null) status = "MISSING_TOBE";
            else if (aLc.equals(tLc)) status = "MATCH";
            else status = "DIFF";

            Row r = sh.createRow(rowIdx++);
            int c = 0;
            createCell(r, c++, svc, bs);
            createCell(r, c++, ns, bs);
            createCell(r, c++, id, bs);
            createCell(r, c++, String.valueOf(i + 1), bs);
            createCell(r, c++, safe(asisType), bs);
            createCell(r, c++, aNm, bs);
            createCell(r, c++, aLc, bs);
            createCell(r, c++, safe(tobeType), bs);
            createCell(r, c++, tNm, bs);
            createCell(r, c++, tLc, bs);
            createCell(r, c++, status, bs);
        }
        return rowIdx;
    }

    private static void setSelectSheetWidths(Sheet sh) {
        sh.setColumnWidth(0, 40 * 256);
        sh.setColumnWidth(1, 60 * 256);
        sh.setColumnWidth(2, 40 * 256);
        sh.setColumnWidth(3, 8 * 256);
        sh.setColumnWidth(4, 30 * 256);
        sh.setColumnWidth(5, 25 * 256);
        sh.setColumnWidth(6, 100 * 256);
        sh.setColumnWidth(7, 40 * 256);
    }

    private static void setSelectCompareSheetWidths(Sheet sh) {
        sh.setColumnWidth(0, 40 * 256);
        sh.setColumnWidth(1, 60 * 256);
        sh.setColumnWidth(2, 40 * 256);
        sh.setColumnWidth(3, 8 * 256);
        sh.setColumnWidth(4, 28 * 256);
        sh.setColumnWidth(5, 25 * 256);
        sh.setColumnWidth(6, 28 * 256);
        sh.setColumnWidth(7, 25 * 256);
        sh.setColumnWidth(8, 18 * 256);
    }

    private static void setDmlSheetWidths(Sheet sh) {
        sh.setColumnWidth(0, 40 * 256);
        sh.setColumnWidth(1, 60 * 256);
        sh.setColumnWidth(2, 40 * 256);
        sh.setColumnWidth(3, 8 * 256);
        sh.setColumnWidth(4, 12 * 256);
        sh.setColumnWidth(5, 28 * 256);
        sh.setColumnWidth(6, 25 * 256);
        sh.setColumnWidth(7, 28 * 256);
    }

    private static void setDmlCompareSheetWidths(Sheet sh) {
        sh.setColumnWidth(0, 40 * 256);
        sh.setColumnWidth(1, 60 * 256);
        sh.setColumnWidth(2, 40 * 256);
        sh.setColumnWidth(3, 8 * 256);
        sh.setColumnWidth(4, 14 * 256);
        sh.setColumnWidth(5, 26 * 256);
        sh.setColumnWidth(6, 22 * 256);
        sh.setColumnWidth(7, 14 * 256);
        sh.setColumnWidth(8, 26 * 256);
        sh.setColumnWidth(9, 22 * 256);
        sh.setColumnWidth(10, 18 * 256);
    }

    private static void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        if (style != null) cell.setCellStyle(style);
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setWrapText(true);
        return style;
    }

    private static CellStyle createBodyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    // ----------------------------------------------------------------
    // Progress / util
    // ----------------------------------------------------------------

    private static void startHeartbeat(int total) {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(30_000L);
                    int done = CURRENT_INDEX.get();
                    System.out.println("[HEARTBEAT] running... " + done + "/" + total + " last=" + CURRENT_KEY);
                }
            } catch (InterruptedException ignored) {
            }
        }, "meta-compare-heartbeat");
        t.setDaemon(true);
        t.start();
    }

    private static void logProgress(int done, int total, int success, int skip,
                                    long loopStartNs, String lastKey) {
        long elapsed = (System.nanoTime() - loopStartNs) / 1_000_000L;
        System.out.printf("[PROGRESS] %d/%d success=%d skip=%d elapsed=%dms last=%s%n",
                done, total, success, skip, elapsed, lastKey);
    }

    private static void mkdirs(Path p) {
        try {
            Files.createDirectories(p);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create directory: " + p, e);
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static long ms(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000L;
    }

    private static int parseInt(String s, int def) {
        if (s == null || s.isBlank()) return def;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static boolean parseBoolean(String s, boolean def) {
        if (s == null || s.isBlank()) return def;
        String v = s.trim().toLowerCase(Locale.ROOT);
        return v.equals("true") || v.equals("1") || v.equals("y") || v.equals("yes");
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new HashMap<>();
        if (args == null) return m;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a == null) continue;
            a = a.trim();
            if (!a.startsWith("--")) continue;

            String k;
            String v;

            int eq = a.indexOf('=');
            if (eq > 2) {
                k = a.substring(2, eq).trim();
                v = a.substring(eq + 1).trim();
            } else {
                k = a.substring(2).trim();
                v = "";
                if (i + 1 < args.length && args[i + 1] != null && !args[i + 1].startsWith("--")) {
                    v = args[i + 1].trim();
                    i++;
                }
            }

            if (!k.isEmpty()) m.put(k, v);
        }

        return m;
    }
}
