package oasys.migration.alias;

import oasys.migration.callchain.ServiceSqlCall;
import oasys.migration.callchain.ServiceSqlXrefLoader;
import oasys.migration.sql.SqlStatement;
import oasys.migration.sql.SqlStatementRegistry;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AliasSqlGenerateCli {

    // ============================================================
    // ✅ System properties (SqlsDirectoryScanner와 동일 키를 가정)
    // ============================================================
    private static final String PROP_BASE_DIR = "oasys.migration.baseDir";
    private static final String PROP_SYSTEM   = "oasys.migration.system";
    private static final String PROP_SQLS_DIR = "oasys.migration.sqlsDir";

    // ✅ pretty option system property key
    private static final String PROP_PRETTY   = "oasys.migration.pretty";

    private static volatile String CURRENT_KEY = "";
    private static final AtomicInteger CURRENT_INDEX = new AtomicInteger(0);

    public static void main(String[] args) {

        long t0 = System.nanoTime();
        Map<String, String> argv = parseArgs(args);

        // ------------------------------------------------------------
        // ✅ baseDir / system / sqlsDir 결정 + system properties 반영
        // ------------------------------------------------------------
        // 1) baseDir (CLI 우선, 없으면 -D, 없으면 jar 위치)
        applyBaseDirPropertyIfPresent(argv);
        Path baseDir = resolveBaseDir();
        ensureBaseDirProperty(baseDir);

        // 2) system (CLI/system property/path 자동 추론)
        String system = resolveSystem(argv);
        ensureSystemProperty(system);

        // 3) sqlsDir (우선순위: -D sqlsDir > --sqlsDir > 기본(system 있으면 systems/<system>/sqls))
        Path sqlsDir = resolveSqlsDir(baseDir, system, argv);
        ensureSqlsDirProperty(sqlsDir);

        // ------------------------------------------------------------
        // mode / input / output
        // ------------------------------------------------------------
        AliasSqlGenerator.Mode mode = parseMode(argv.getOrDefault("mode", "ASIS_ALIAS_TO_TOBE"));

        // system 프리셋: 옵션이 없을 때만 기본값으로 채움
        String callchainCsv = argv.containsKey("csv")
                ? argv.get("csv")
                : (system != null ? "systems/" + system + "/mapping/service_sql_xref.csv" : "mapping/service_sql_xref.csv");

        String columnMappingXlsx = argv.containsKey("mapping")
                ? argv.get("mapping")
                : (system != null ? "systems/" + system + "/mapping/column_mapping.xlsx" : "mapping/column_mapping.xlsx");

        String defaultOut = (mode == AliasSqlGenerator.Mode.TOBE_SQL)
                ? (system != null ? "output/" + system + "/tobe-sql" : "output/tobe-sql")
                : (system != null ? "output/" + system + "/alias-sql" : "output/alias-sql");

        String defaultResult = (mode == AliasSqlGenerator.Mode.TOBE_SQL)
                ? (system != null ? "output/" + system + "/tobe-sql.xlsx" : "output/tobe-sql.xlsx")
                : (system != null ? "output/" + system + "/alias-sql-result.xlsx" : "output/alias-sql-result.xlsx");

        Path outputSqlDir = resolvePath(baseDir, argv.getOrDefault("out", defaultOut));
        Path resultXlsx   = resolvePath(baseDir, argv.getOrDefault("result", defaultResult));

        // 파일 입력: baseDir 기준 절대경로로 변환
        Path callchainCsvPath = resolvePath(baseDir, callchainCsv);
        Path mappingXlsxPath  = resolvePath(baseDir, columnMappingXlsx);

        int max = parseInt(argv.get("max"), -1);
        int logEvery = parseInt(argv.get("logEvery"), 100);
        long slowMs = parseLong(argv.get("slowMs"), 500L);
        boolean failFast = parseBoolean(argv.get("failFast"), false);
        boolean logFallback = parseBoolean(argv.get("logFallback"), false);

        // ------------------------------------------------------------
        // ✅ SQL Prettier option (기본 ON)
        //   우선순위: --pretty > -Doasys.migration.pretty > default(true)
        // ------------------------------------------------------------
        String prettyRaw = argv.containsKey("pretty")
                ? argv.get("pretty")
                : System.getProperty(PROP_PRETTY, "true");

        boolean pretty = parseBoolean(prettyRaw, true);
        System.setProperty(PROP_PRETTY, String.valueOf(pretty));

        System.out.println("==================================================");
        System.out.println("[START] Alias SQL generation");
        System.out.println("[CONF] baseDir     = " + baseDir.toAbsolutePath());
        System.out.println("[CONF] system      = " + (system == null ? "" : system));
        System.out.println("[CONF] sqlsDir     = " + sqlsDir.toAbsolutePath());
        System.out.println("[CONF] mode        = " + mode);
        System.out.println("[CONF] pretty      = " + pretty + " (use --pretty=false or -D" + PROP_PRETTY + "=false)");
        System.out.println("[CONF] csv         = " + callchainCsvPath.toAbsolutePath());
        System.out.println("[CONF] mapping     = " + mappingXlsxPath.toAbsolutePath());
        System.out.println("[CONF] out         = " + outputSqlDir.toAbsolutePath());
        System.out.println("[CONF] result      = " + resultXlsx.toAbsolutePath());
        System.out.println("[CONF] max         = " + max);
        System.out.println("[CONF] logEvery    = " + logEvery);
        System.out.println("[CONF] slowMs      = " + slowMs);
        System.out.println("[CONF] failFast    = " + failFast);
        System.out.println("[CONF] logFallback = " + logFallback);
        System.out.println("==================================================");

        // 간단 선검증 (없으면 여기서 바로 안내)
        validateFileExists(callchainCsvPath, "xref csv (--csv)");
        validateFileExists(mappingXlsxPath, "column mapping xlsx (--mapping)");
        // sqlsDir는 SqlsDirectoryScanner에서 추가로 자세히 검증/예외낼 수 있지만, 여기서도 빠르게 체크
        if (!Files.exists(sqlsDir) || !Files.isDirectory(sqlsDir)) {
            System.out.println("[WARN] sqlsDir not found or not a directory: " + sqlsDir.toAbsolutePath());
            System.out.println("       - sqls 폴더에 MyBatis mapper *.xml 파일을 복사해 넣어주세요.");
            System.out.println("       - 또는 JVM 옵션: -D" + PROP_SQLS_DIR + "=<sqls폴더경로>");
        }

        mkdirs(outputSqlDir);
        if (resultXlsx.getParent() != null) mkdirs(resultXlsx.getParent());

        // SQL registry (fallback용)
        long tSqlIdx0 = System.nanoTime();
        SqlStatementRegistry sqlRegistry = new SqlStatementRegistry();
        sqlRegistry.initialize();
        System.out.println("[STEP1] SqlStatementRegistry.initialize() done. elapsed=" + ms(tSqlIdx0) + "ms");

        // column mapping
        long tMap0 = System.nanoTime();
        ColumnMappingRegistry columnMappingRegistry = new ColumnMappingRegistry(mappingXlsxPath.toString());
        System.out.println("[STEP2] ColumnMappingRegistry loaded. elapsed=" + ms(tMap0) + "ms");
        System.out.println("[INIT] Column mapping size = " + columnMappingRegistry.size());

        // xref csv
        long tCsv0 = System.nanoTime();
        System.out.println("[STEP3] loading xref csv...");
        ServiceSqlXrefLoader xrefLoader = new ServiceSqlXrefLoader();
        List<ServiceSqlCall> calls = xrefLoader.load(callchainCsvPath.toString());
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
            String ns  = safe(invokeString(call, "getMapperNamespace"));
            String id  = safe(invokeString(call, "getSqlId"));

            CURRENT_KEY = svc + " | " + ns + "." + id;

            long one0 = System.nanoTime();

            // ✅ 1) CSV에 sql_text가 있으면 우선 사용
            String sqlText = invokeString(call, "getSqlText");

            // ✅ 2) 없으면 registry에서 fallback
            if (sqlText == null || sqlText.isBlank()) {
                SqlStatement stmt = null;
                try { stmt = sqlRegistry.get(ns, id); } catch (Exception ignored) {}
                if (stmt != null) {
                    sqlText = stmt.getSqlText();
                    if (logFallback) {
                        System.out.println("[FALLBACK] " + CURRENT_KEY + " <= " + safe(stmt.getMapperFile()));
                    }
                }
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

    // ------------------------------------------------------------
    // ✅ baseDir / system / sqlsDir resolve
    // ------------------------------------------------------------

    private static void applyBaseDirPropertyIfPresent(Map<String, String> argv) {
        String baseDirArg = trimToNull(argv.get("baseDir"));
        if (baseDirArg == null) return;

        // JVM -D가 이미 있으면 우선
        if (!isBlank(System.getProperty(PROP_BASE_DIR))) return;

        // CLI --baseDir 는 "현재 작업 디렉터리(user.dir)" 기준으로 해석
        Path bd = resolveAgainstUserDir(baseDirArg);
        System.setProperty(PROP_BASE_DIR, bd.toString());
    }

    private static void ensureBaseDirProperty(Path baseDir) {
        if (isBlank(System.getProperty(PROP_BASE_DIR)) && baseDir != null) {
            System.setProperty(PROP_BASE_DIR, baseDir.toString());
        }
    }

    private static String resolveSystem(Map<String, String> argv) {
        // 1) CLI --system
        String s = trimToNull(argv.get("system"));
        if (s != null) return s;

        // 2) JVM -Doasys.migration.system
        s = trimToNull(System.getProperty(PROP_SYSTEM));
        if (s != null) return s;

        // 3) path 자동추론: csv/mapping/sqlsDir 에 systems/<system>/... 가 있으면 추론
        s = inferSystemFromPaths(
                argv.get("csv"),
                argv.get("mapping"),
                argv.get("sqlsDir")
        );
        return trimToNull(s);
    }

    private static void ensureSystemProperty(String system) {
        if (system == null) return;
        if (isBlank(System.getProperty(PROP_SYSTEM))) {
            System.setProperty(PROP_SYSTEM, system);
        }
    }

    private static Path resolveSqlsDir(Path baseDir, String system, Map<String, String> argv) {
        // 우선순위: JVM -D > CLI --sqlsDir > 기본
        String raw = trimToNull(System.getProperty(PROP_SQLS_DIR));
        if (raw == null) raw = trimToNull(argv.get("sqlsDir"));

        if (raw == null) {
            raw = (system != null) ? ("systems/" + system + "/sqls") : "sqls";
        }

        return resolvePath(baseDir, raw);
    }

    private static void ensureSqlsDirProperty(Path sqlsDir) {
        if (sqlsDir == null) return;
        // JVM -D가 이미 있으면 그대로 존중(덮어쓰지 않음)
        if (isBlank(System.getProperty(PROP_SQLS_DIR))) {
            System.setProperty(PROP_SQLS_DIR, sqlsDir.toString());
        }
    }

    private static String inferSystemFromPaths(String... paths) {
        if (paths == null) return null;
        for (String p : paths) {
            String sys = inferSystemFromPath(p);
            if (sys != null) return sys;
        }
        return null;
    }

    private static String inferSystemFromPath(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        // file: URI면 path 부분만 보기
        if (s.startsWith("file:")) {
            try {
                Path p = Path.of(URI.create(s));
                s = p.toString();
            } catch (Exception ignored) {}
        }

        String n = s.replace('\\', '/');
        String lower = n.toLowerCase(Locale.ROOT);

        int idx = lower.indexOf("/systems/");
        if (idx < 0) {
            // 상대경로로 "systems/..." 형태
            if (lower.startsWith("systems/")) idx = -1;
            else return null;
        }

        String cut = (idx >= 0) ? n.substring(idx + "/systems/".length()) : n.substring("systems/".length());
        // cut = "<system>/..."
        int slash = cut.indexOf('/');
        if (slash <= 0) return null;

        String sys = cut.substring(0, slash).trim();
        if (sys.isEmpty()) return null;

        // 시스템명은 안전하게 (영문/숫자/대시/언더스코어)만 허용
        if (!sys.matches("[A-Za-z0-9_-]{1,40}")) return null;
        return sys;
    }

    private static Path resolveBaseDir() {
        // 1) -Doasys.migration.baseDir
        String baseDir = System.getProperty(PROP_BASE_DIR);
        if (!isBlank(baseDir)) {
            try { return Path.of(baseDir).toAbsolutePath().normalize(); }
            catch (Exception ignored) {}
        }

        // 2) jar 파일 위치(라이브러리로 로딩된 경우 포함)
        try {
            var cs = AliasSqlGenerateCli.class.getProtectionDomain().getCodeSource();
            if (cs != null && cs.getLocation() != null) {
                Path codePath = Path.of(cs.getLocation().toURI()).toAbsolutePath().normalize();
                String p = codePath.toString().toLowerCase(Locale.ROOT);
                if (Files.isRegularFile(codePath) && p.endsWith(".jar")) {
                    Path parent = codePath.getParent();
                    if (parent != null) return parent.toAbsolutePath().normalize();
                }
            }
        } catch (Exception ignored) {}

        // 3) fallback: 현재 작업 디렉터리
        return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    }

    private static Path resolveAgainstUserDir(String raw) {
        Path p = Path.of(raw.trim());
        if (p.isAbsolute()) return p.toAbsolutePath().normalize();
        return Path.of(System.getProperty("user.dir", ".")).resolve(p).toAbsolutePath().normalize();
    }

    private static Path resolvePath(Path baseDir, String input) {
        if (input == null) return baseDir;
        String s = input.trim();

        if (s.startsWith("file:")) {
            try { return Path.of(URI.create(s)).toAbsolutePath().normalize(); }
            catch (Exception ignored) {}
        }

        Path p = Path.of(s);
        if (p.isAbsolute()) return p.toAbsolutePath().normalize();
        return baseDir.resolve(p).toAbsolutePath().normalize();
    }

    private static void validateFileExists(Path p, String label) {
        if (p == null) throw new IllegalArgumentException(label + " is null");
        if (!Files.exists(p) || !Files.isRegularFile(p)) {
            throw new IllegalStateException("File not found: " + p.toAbsolutePath() + " (" + label + ")");
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ------------------------------------------------------------
    // existing util/logging
    // ------------------------------------------------------------

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
