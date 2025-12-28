package oasys.migration.sql;

import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SqlsDirectoryScanner {

    public static final String PROP_BASE_DIR = "oasys.migration.baseDir";
    public static final String PROP_SYSTEM   = "oasys.migration.system";
    public static final String PROP_SQLS_DIR = "oasys.migration.sqlsDir";

    private Path resolvedBaseDir;
    private Path resolvedSqlsDir;

    /**
     * ✅ 외부(sqls) 디렉터리 전체 XML 파일 스캔
     *
     * 우선순위
     * 1) -Doasys.migration.sqlsDir=/path/to/sqls  (또는 CLI에서 --sqlsDir 로 SystemProperty 설정)
     * 2) (baseDir)/systems/<system>/sqls         (system이 있을 때)
     * 3) (baseDir)/sqls
     *
     * baseDir 우선순위
     * 1) -Doasys.migration.baseDir=/path   (또는 CLI에서 --baseDir 로 SystemProperty 설정)
     * 2) 실행 jar 파일이 있으면 jar의 부모 디렉터리
     * 3) user.dir
     */
    public List<Path> scanAllSqlXml() {

        List<Path> result = new ArrayList<>();

        try {
            resolvedBaseDir = resolveBaseDir();
            resolvedSqlsDir = resolveSqlsDir(resolvedBaseDir);

            // ✅ 디렉터리 없으면 생성 (사용자가 xml을 첨부할 위치 확보)
            if (!Files.exists(resolvedSqlsDir)) {
                Files.createDirectories(resolvedSqlsDir);
            }

            if (!Files.isDirectory(resolvedSqlsDir)) {
                throw new IllegalStateException("sqls path is not a directory: " + resolvedSqlsDir.toAbsolutePath());
            }

            Files.walk(resolvedSqlsDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".xml"))
                    .forEach(result::add);

            if (result.isEmpty()) {
                throw new IllegalStateException(
                        "No mapper xml found under sqls directory: " + resolvedSqlsDir.toAbsolutePath()
                                + "\n- sqls 폴더에 MyBatis mapper *.xml 파일을 복사해 넣어주세요."
                                + "\n- 또는 CLI 옵션 --sqlsDir=<sqls폴더경로> 를 지정하세요."
                                + "\n- 또는 JVM 옵션으로 -D" + PROP_SQLS_DIR + "=<sqls폴더경로> 를 지정하세요."
                );
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to scan sqls directory", e);
        }
    }

    public Path getResolvedBaseDir() {
        return resolvedBaseDir;
    }

    public Path getResolvedSqlsDir() {
        return resolvedSqlsDir;
    }

    private Path resolveSqlsDir(Path baseDir) {

        // 1) explicit override (JVM property)
        String sqlsDir = System.getProperty(PROP_SQLS_DIR);
        if (sqlsDir != null && !sqlsDir.isBlank()) {
            return resolveAgainstBaseDir(baseDir, sqlsDir);
        }

        // 2) system preset: baseDir/systems/<system>/sqls
        String system = System.getProperty(PROP_SYSTEM);
        if (system != null && !system.isBlank()) {
            return baseDir.resolve("systems").resolve(system.trim()).resolve("sqls").toAbsolutePath().normalize();
        }

        // 3) baseDir/sqls
        return baseDir.resolve("sqls").toAbsolutePath().normalize();
    }

    private Path resolveAgainstBaseDir(Path baseDir, String raw) {
        String loc = raw.trim();

        // file:/ URI
        if (loc.startsWith("file:")) {
            try {
                return Path.of(URI.create(loc)).toAbsolutePath().normalize();
            } catch (Exception e) {
                // fallback below
            }
        }

        Path p = Path.of(loc);
        if (p.isAbsolute()) {
            return p.toAbsolutePath().normalize();
        }
        return baseDir.resolve(p).toAbsolutePath().normalize();
    }

    private Path resolveBaseDir() {

        // 1) explicit override
        String baseDir = System.getProperty(PROP_BASE_DIR);
        if (baseDir != null && !baseDir.isBlank()) {
            try {
                return Path.of(baseDir).toAbsolutePath().normalize();
            } catch (Exception ignored) {
            }
        }

        // 2) jar location (when running as jar / library)
        try {
            var cs = SqlsDirectoryScanner.class.getProtectionDomain().getCodeSource();
            if (cs != null && cs.getLocation() != null) {
                Path codePath = Path.of(cs.getLocation().toURI()).toAbsolutePath().normalize();
                String p = codePath.toString().toLowerCase(Locale.ROOT);

                if (Files.isRegularFile(codePath) && p.endsWith(".jar")) {
                    Path parent = codePath.getParent();
                    if (parent != null) return parent.toAbsolutePath().normalize();
                }
            }
        } catch (Exception ignored) {
        }

        // 3) fallback
        return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    }
}
