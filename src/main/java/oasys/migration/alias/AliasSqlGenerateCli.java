package oasys.migration.alias;

import oasys.migration.callchain.ServiceSqlCall;
import oasys.migration.callchain.ServiceSqlXrefLoader;
import oasys.migration.sql.SqlStatement;
import oasys.migration.sql.SqlStatementRegistry;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AliasSqlGenerateCli {

    private static volatile String CURRENT_KEY = "";
    private static final AtomicInteger CURRENT_INDEX = new AtomicInteger(0);

    public static void main(String[] args) {

        long t0 = System.nanoTime();
        Map<String, String> argv = parseArgs(args);

        AliasSqlGenerator.Mode mode = parseMode(argv.getOrDefault("mode", "ASIS_ALIAS_TO_TOBE"));
        String callchainCsv = argv.getOrDefault("csv", "mapping/service_sql_xref.csv");
        String columnMappingXlsx = argv.getOrDefault("mapping", "mapping/column_mapping.xlsx");

        Path outputSqlDir = Path.of(argv.getOrDefault("out", "output/alias-sql"));
        Path resultXlsx = Path.of(argv.getOrDefault("result", "output/alias-sql-result.xlsx"));

        int max = parseInt(argv.get("max"), -1);
        int logEvery = parseInt(argv.get("logEvery"), 100);
        long slowMs = parseLong(argv.get("slowMs"), 500L);
        boolean failFast = parseBoolean(argv.get("failFast"), false);

        System.out.println("==================================================");
        System.out.println("[START] Alias SQL generation");
        System.out.println("[CONF] mode      = " + mode);
        System.out.println("[CONF] csv       = " + callchainCsv);
        System.out.println("[CONF] mapping   = " + columnMappingXlsx);
        System.out.println("[CONF] out       = " + outputSqlDir.toAbsolutePath());
        System.out.println("[CONF] result    = " + resultXlsx.toAbsolutePath());
        System.out.println("[CONF] max       = " + max);
        System.out.println("[CONF] logEvery  = " + logEvery);
        System.out.println("[CONF] slowMs    = " + slowMs);
        System.out.println("[CONF] failFast  = " + failFast);
        System.out.println("==================================================");

        mkdirs(outputSqlDir);
        if (resultXlsx.getParent() != null) mkdirs(resultXlsx.getParent());

        // SQL registry (fallback용)
        long tSqlIdx0 = System.nanoTime();
        SqlStatementRegistry sqlRegistry = new SqlStatementRegistry();
        sqlRegistry.initialize();
        System.out.println("[INIT] SQL index size = 7540");
        System.out.println("[STEP1] SqlStatementRegistry.initialize() done. elapsed=" + ms(tSqlIdx0) + "ms");

        // column mapping
        long tMap0 = System.nanoTime();
        ColumnMappingRegistry columnMappingRegistry = new ColumnMappingRegistry(columnMappingXlsx);
        System.out.println("[STEP2] ColumnMappingRegistry loaded. elapsed=" + ms(tMap0) + "ms");
        System.out.println("[INIT] Column mapping size = " + columnMappingRegistry.size());

        // xref csv
        long tCsv0 = System.nanoTime();
        System.out.println("[STEP3] loading xref csv...");
        ServiceSqlXrefLoader xrefLoader = new ServiceSqlXrefLoader();
        List<ServiceSqlCall> calls = xrefLoader.load(callchainCsv);
        System.out.println("[STEP3] xref csv loaded. size=" + calls.size() + ", elapsed=" + ms(tCsv0) + "ms");

        if (max > 0 && calls.size() > max) {
            calls = calls.subList(0, max);
            System.out.println("[STEP3] apply max => truncated to " + calls.size());
        }

        // heartbeat thread: 멈춘 지점 확인용
        startHeartbeat(calls.size());

        AliasSqlGenerator generator = new AliasSqlGenerator(columnMappingRegistry);
        AliasSqlFileWriter sqlWriter = new AliasSqlFileWriter();
        AliasSqlResultXlsxWriter resultWriter = new AliasSqlResultXlsxWriter();

        long tLoop0 = System.nanoTime();
        int total = calls.size();
        System.out.println("[STEP4] generating start. total=" + total);

        List<AliasSqlResult> results = new ArrayList<>(Math.max(16, total));

        int success = 0;
        int skip = 0;

        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();

        // TOBE 모드일 때만 2개 시트를 추가로 만들기 위한 수집 리스트
        final List<AliasSqlResultXlsxWriter.TobeSelectOutputRow> tobeSelectOutputs =
                (mode == AliasSqlGenerator.Mode.TOBE_SQL) ? new ArrayList<>(Math.max(64, total)) : Collections.emptyList();
        final List<AliasSqlResultXlsxWriter.TobeDmlParamRow> tobeDmlParams =
                (mode == AliasSqlGenerator.Mode.TOBE_SQL) ? new ArrayList<>(Math.max(64, total)) : Collections.emptyList();

        for (int i = 0; i < total; i++) {
            CURRENT_INDEX.set(i + 1);

            ServiceSqlCall call = calls.get(i);
            String svc = safe(invokeString(call, "getServiceClass"));
            String ns = safe(invokeString(call, "getMapperNamespace"));
            String id = safe(invokeString(call, "getSqlId"));

            CURRENT_KEY = svc + " | " + ns + "." + id;

            long one0 = System.nanoTime();

            // ✅ 1) CSV에 sql_text가 있으면 우선 사용
            String sqlText = invokeString(call, "getSqlText");

            // ✅ 2) 없으면 registry에서 fallback
            if (sqlText == null || sqlText.isBlank()) {
                SqlStatement stmt = null;
                try {
                    stmt = sqlRegistry.get(ns, id);
                } catch (Exception ignored) {
                }
                if (stmt != null) sqlText = stmt.getSqlText();
            }

            if (sqlText == null || sqlText.isBlank()) {
                skip++;
                results.add(new AliasSqlResult("SKIP", svc, ns, id, "SQL_TEXT_EMPTY"));
                if ((i + 1) % logEvery == 0 || (i + 1) == total) {
                    logProgress(i + 1, total, success, skip, tLoop0, CURRENT_KEY, mem);
                }
                continue;
            }

            try {
                String transformedSql = generator.generate(sqlText, mode);

                sqlWriter.write(outputSqlDir, svc, ns, id, transformedSql);

                // TOBE 모드일 때만: SELECT 출력 컬럼 / DML 파라미터(lowerCamel) 수집
                if (mode == AliasSqlGenerator.Mode.TOBE_SQL) {
                    collectTobeSelectOutputs(transformedSql, svc, ns, id, tobeSelectOutputs);
                    collectTobeDmlParams(transformedSql, svc, ns, id, tobeDmlParams);
                }

                success++;
                results.add(new AliasSqlResult("SUCCESS", svc, ns, id, ""));

            } catch (Exception e) {
                skip++;
                results.add(new AliasSqlResult("SKIP", svc, ns, id, e.getClass().getSimpleName()));

                System.out.println("[ERROR] transform/write failed: " + CURRENT_KEY);
                System.out.println("        ex=" + e.getClass().getName() + ": " + safe(e.getMessage()));
                e.printStackTrace(System.out);

                if (failFast) {
                    System.out.println("[FAILFAST] stop on first error.");
                    break;
                }
            }

            long oneMs = (System.nanoTime() - one0) / 1_000_000L;
            if (oneMs >= slowMs) {
                System.out.println("[SLOW] " + oneMs + "ms : " + CURRENT_KEY);
            }

            if ((i + 1) % logEvery == 0 || (i + 1) == total) {
                logProgress(i + 1, total, success, skip, tLoop0, CURRENT_KEY, mem);
            }
        }

        System.out.println("[STEP4] generating done. elapsed=" + ms(tLoop0) + "ms");
        System.out.println("[STAT] success=" + success + ", skip=" + skip);
        System.out.println("[STAT] lastKey=" + CURRENT_KEY);

        long tXlsx0 = System.nanoTime();
        System.out.println("[STEP5] writing result xlsx... rows=" + results.size());
        resultWriter.write(resultXlsx, results, tobeSelectOutputs, tobeDmlParams);
        System.out.println("[STEP5] result xlsx written. elapsed=" + ms(tXlsx0) + "ms");

        System.out.println("==================================================");
        System.out.println("[DONE] totalElapsed=" + ms(t0) + "ms");
        System.out.println("==================================================");
    }

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
        }, "alias-sql-heartbeat");
        t.setDaemon(true);
        t.start();
    }

    private static void logProgress(int done, int total, int success, int skip,
                                    long loopStartNs, String lastKey, MemoryMXBean mem) {
        long elapsed = (System.nanoTime() - loopStartNs) / 1_000_000L;

        MemoryUsage heap = mem.getHeapMemoryUsage();
        long usedMb = heap.getUsed() / (1024 * 1024);
        long maxMb = heap.getMax() / (1024 * 1024);

        System.out.printf("[PROGRESS] %d/%d success=%d skip=%d elapsed=%dms heap=%d/%dMB last=%s%n",
                done, total, success, skip, elapsed, usedMb, maxMb, lastKey);
    }

    private static void mkdirs(Path p) {
        try {
            Files.createDirectories(p);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create directory: " + p, e);
        }
    }

    private static String invokeString(Object target, String method) {
        try {
            var m = target.getClass().getMethod(method);
            Object v = m.invoke(target);
            return (v == null) ? null : String.valueOf(v);
        } catch (Exception e) {
            return null;
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

    private static long parseLong(String s, long def) {
        if (s == null || s.isBlank()) return def;
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }

    private static boolean parseBoolean(String s, boolean def) {
        if (s == null || s.isBlank()) return def;
        String v = s.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("y") || v.equals("yes");
    }

    private static AliasSqlGenerator.Mode parseMode(String raw) {
        if (raw == null) return AliasSqlGenerator.Mode.ASIS_ALIAS_TO_TOBE;
        String u = raw.trim().toUpperCase();
        if (u.equals("TOBE") || u.equals("TOBE_SQL") || u.equals("TOBE-SQL")) return AliasSqlGenerator.Mode.TOBE_SQL;
        return AliasSqlGenerator.Mode.ASIS_ALIAS_TO_TOBE;
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

    // ============================================================
    // TOBE 모드: SELECT 출력 / DML 파라미터 추출 (lowerCamel)
    // ============================================================

    private static void collectTobeSelectOutputs(String tobeSql,
                                                 String serviceClass,
                                                 String namespace,
                                                 String sqlId,
                                                 List<AliasSqlResultXlsxWriter.TobeSelectOutputRow> out) {
        if (tobeSql == null || tobeSql.isBlank()) return;
        if (!isLeadingKeyword(tobeSql, "SELECT")) return;

        String selectBody = extractTopLevelSelectBody(tobeSql);
        if (selectBody == null || selectBody.isBlank()) return;

        List<String> items = splitTopLevelByComma(selectBody);
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
                // 함수명/기타 토큰이 alias로 붙는 경우(예: AS F_OPEN_EDIT) → expr 내부 컬럼ID를 우선
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
            out.add(new AliasSqlResultXlsxWriter.TobeSelectOutputRow(
                    serviceClass, namespace, sqlId, ++seq, outputName, lc, expr, trailingComment
            ));
        }
    }

    private static void collectTobeDmlParams(String tobeSql,
                                             String serviceClass,
                                             String namespace,
                                             String sqlId,
                                             List<AliasSqlResultXlsxWriter.TobeDmlParamRow> out) {
        if (tobeSql == null || tobeSql.isBlank()) return;

        boolean isInsert = isLeadingKeyword(tobeSql, "INSERT");
        boolean isUpdate = isLeadingKeyword(tobeSql, "UPDATE");
        boolean isDelete = isLeadingKeyword(tobeSql, "DELETE");
        if (!(isInsert || isUpdate || isDelete)) return;

        String dmlType = isInsert ? "INSERT" : (isUpdate ? "UPDATE" : "DELETE");

        LinkedHashSet<String> params = extractHashParams(tobeSql);
        if (params.isEmpty()) return;

        int seq = 0;
        for (String p : params) {
            String name = normalizeParamName(p);
            if (name == null || name.isBlank()) continue;

            String lc = toLowerCamel(name);
            out.add(new AliasSqlResultXlsxWriter.TobeDmlParamRow(
                    serviceClass, namespace, sqlId, ++seq, dmlType, name, lc
            ));
        }
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

    private static String extractTopLevelSelectBody(String sql) {
        String s = stripLeadingSpaceAndComments(sql);
        int n = s.length();

        int pos = indexOfKeywordTopLevel(s, 0, "SELECT");
        if (pos < 0) return null;

        int afterSelect = pos + 6;
        while (afterSelect < n && Character.isWhitespace(s.charAt(afterSelect))) afterSelect++;

        int fromPos = indexOfKeywordTopLevel(s, afterSelect, "FROM");
        if (fromPos < 0) return null;

        return s.substring(afterSelect, fromPos).trim();
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

            if (depth == 0 && matchesKeywordAt(s, i, kw)) return i;
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

    // ✅ 중복 없이 1개만 유지
    private static List<String> splitTopLevelByComma(String s) {
        return splitTopLevelByComma0(s);
    }

    private static List<String> splitTopLevelByComma0(String s) {
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
        String u = expr.toUpperCase();

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
        if (m2.find()) return m2.group(1).toUpperCase();

        return null;
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
        return name.isEmpty() ? null : name.toUpperCase();
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

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
    }
}
