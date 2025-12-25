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
        resultWriter.write(resultXlsx, results);
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
}
