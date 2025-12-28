package oasys.migration.alias;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

public class AliasSqlFileWriter {

    /** 출력 경로 레이아웃 */
    public enum OutLayout {
        SIMPLE,  // <out>/<groupKey>/<sqlId>.sql
        LEGACY   // <out>/<serviceClass>/<namespace>/<sqlId>.sql (기존)
    }

    /** 시스템 프로퍼티 키 */
    public static final String PROP_OUT_LAYOUT = "oasys.migration.outLayout";

    public void write(Path outDir, String serviceClass, String namespace, String sqlId, String sqlText) {
        Path file = resolveOutFile(outDir, serviceClass, namespace, sqlId, getOutLayout());
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Files.writeString(
                    file,
                    sqlText == null ? "" : sqlText,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to write sql file: " + file, e);
        }
    }

    /** 현재 설정된 레이아웃 */
    public static OutLayout getOutLayout() {
        String v = System.getProperty(PROP_OUT_LAYOUT, "simple");
        if (v == null) return OutLayout.SIMPLE;
        v = v.trim().toLowerCase(Locale.ROOT);
        return (v.equals("legacy") || v.equals("old")) ? OutLayout.LEGACY : OutLayout.SIMPLE;
    }

    /** 레이아웃에 따라 출력 파일 경로 계산 */
    public static Path resolveOutFile(Path outDir, String serviceClass, String namespace, String sqlId, OutLayout layout) {
        if (outDir == null) outDir = Path.of(".");

        if (layout == OutLayout.LEGACY) {
            // 기존 규칙 유지(호환)
            String svcDir = toLegacySafePath(serviceClass, true);
            String nsDir  = toLegacySafePath(namespace, true);
            String file   = toLegacySafePath(sqlId, false) + ".sql";
            return outDir.resolve(svcDir).resolve(nsDir).resolve(file);
        }

        // ✅ 단순 규칙: <out>/<groupKey>/<sqlId>.sql
        // 단, 파일명(sqlId)에서 namespace/package가 붙는 경우 제거하여
        // AEPAFU001EMapper_insertXXXX.sql 형태로 정규화한다.
        String groupKey = deriveGroupKey(serviceClass, namespace);

        String normalizedSqlId = normalizeSqlIdForFileName(namespace, sqlId);
        String fileName = toSafeFileName(normalizedSqlId) + ".sql";

        return outDir.resolve(groupKey).resolve(fileName);
    }

    /** (권장) MigrationVerifyCli 등에서 사용: 기본 레이아웃으로 경로 계산 */
    public static Path resolveOutFile(Path outDir, String serviceClass, String namespace, String sqlId) {
        return resolveOutFile(outDir, serviceClass, namespace, sqlId, getOutLayout());
    }

    /**
     * groupKey 규칙:
     * 1) serviceClass 에서 ServiceImpl/Service/Impl 떼고 소문자로 (예: AEPAFU001ServiceImpl -> aepafu001)
     * 2) 없으면 namespace 마지막 3세그먼트로 조합
     */
    public static String deriveGroupKey(String serviceClass, String namespace) {
        String base = lastSegment(serviceClass);
        base = stripSuffix(base, "ServiceImpl");
        base = stripSuffix(base, "Service");
        base = stripSuffix(base, "Impl");

        base = base == null ? "" : base.trim();
        if (!base.isEmpty()) {
            return toSafeDirName(base.toLowerCase(Locale.ROOT));
        }

        // fallback: namespace
        String ns = namespace == null ? "" : namespace.trim();
        ns = ns.replace('\\', '/').replace('.', '/').replaceAll("/+", "/");
        String[] parts = ns.split("/");
        StringBuilder sb = new StringBuilder();

        // 마지막 3개 세그먼트 우선 사용
        int take = Math.min(3, parts.length);
        for (int i = parts.length - take; i < parts.length; i++) {
            String p = (i >= 0 && i < parts.length) ? parts[i] : "";
            if (p == null) continue;
            p = p.trim();
            if (p.isEmpty()) continue;
            sb.append(p.toLowerCase(Locale.ROOT));
        }

        String k = sb.toString();
        if (k.isEmpty()) k = "unknown";
        return toSafeDirName(k);
    }

    // -----------------------
    // ✅ FILE NAME NORMALIZER
    // -----------------------

    /**
     * sqlId가 "kr.go....AEPAFU001EMapper_insertXXXX" 처럼 namespace(FQCN)까지 포함되는 경우,
     * 파일명은 "AEPAFU001EMapper_insertXXXX" 로 정규화한다.
     *
     * - 우선순위:
     *   1) (namespace가 있고) sqlId가 namespace로 시작하면: (namespace 제거) + (MapperSimple + "_" + statement)
     *   2) 아니면 sqlId에서 MapperSimple 마지막 등장 위치를 기준으로 뒤를 statement로 간주
     *   3) 그래도 못 만들면 sqlId 그대로 반환
     */
    private static String normalizeSqlIdForFileName(String namespace, String sqlId) {
        String id = (sqlId == null) ? "" : sqlId.trim();
        if (id.isEmpty()) return "_";

        String ns = (namespace == null) ? "" : namespace.trim();
        String mapperSimple = lastSegment(ns);
        if (mapperSimple == null) mapperSimple = "";
        mapperSimple = mapperSimple.trim();

        if (mapperSimple.isEmpty()) {
            mapperSimple = guessMapperSimpleFromSqlId(id);
        }

        String stmt = null;

        // 1) namespace로 시작하는 패턴 (가장 정확)
        if (!ns.isEmpty()) {
            if (id.startsWith(ns)) {
                stmt = stripLeadingDelims(id.substring(ns.length()));
            } else {
                // sqlId가 이미 '_' 형태로 들어오는 경우도 방어
                String nsSafe = ns.replace('\\', '_').replace('/', '_').replace('.', '_');
                if (id.startsWith(nsSafe)) {
                    stmt = stripLeadingDelims(id.substring(nsSafe.length()));
                }
            }
        }

        // 2) mapperSimple 기준으로 뒤를 statement로 추정
        if ((stmt == null || stmt.isEmpty()) && mapperSimple != null && !mapperSimple.isEmpty()) {
            int idx = id.lastIndexOf(mapperSimple);
            if (idx >= 0) {
                stmt = stripLeadingDelims(id.substring(idx + mapperSimple.length()));
            }
        }

        if (mapperSimple != null && !mapperSimple.isEmpty() && stmt != null && !stmt.isEmpty()) {
            // stmt가 "insertX" / "selectY" 형태
            return mapperSimple + "_" + stmt;
        }

        // 3) fallback: 원본 유지
        return id;
    }

    private static String stripLeadingDelims(String s) {
        if (s == null) return "";
        String t = s.trim();
        int i = 0;
        while (i < t.length()) {
            char c = t.charAt(i);
            if (c == '.' || c == '_' || c == '-' || c == ':' || c == '/' || c == '\\') {
                i++;
                continue;
            }
            break;
        }
        return (i >= t.length()) ? "" : t.substring(i);
    }

    private static String guessMapperSimpleFromSqlId(String sqlId) {
        if (sqlId == null) return "";
        String s = sqlId.trim();
        if (s.isEmpty()) return "";

        // 마지막 세그먼트(점/경로 기준)
        int cut = Math.max(s.lastIndexOf('.'), Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\')));
        String last = (cut >= 0 && cut + 1 < s.length()) ? s.substring(cut + 1) : s;

        // "AEPAFU001EMapper_insertX" -> "AEPAFU001EMapper"
        int mapperPos = last.indexOf("Mapper");
        if (mapperPos >= 0) {
            return last.substring(0, mapperPos + "Mapper".length());
        }

        int us = last.indexOf('_');
        if (us > 0) return last.substring(0, us);

        return last;
    }

    // -----------------------
    // SAFE NAME HELPERS
    // -----------------------

    private static String toSafeDirName(String raw) {
        if (raw == null || raw.isBlank()) return "unknown";
        String s = raw.trim();

        // Windows/Unix 공통으로 위험한 문자 제거
        s = s.replaceAll("[:*?\"<>|]", "_");
        s = s.replaceAll("\\s+", "_");
        s = s.replaceAll("/+", "_");
        s = s.replaceAll("\\\\+", "_");

        // 너무 길면 잘라냄(윈도 경로 길이/가독성)
        if (s.length() > 120) s = s.substring(0, 120);

        return s.isBlank() ? "unknown" : s;
    }

    private static String toSafeFileName(String raw) {
        if (raw == null || raw.isBlank()) return "_";
        String s = raw.trim();

        // 파일명에 슬래시/역슬래시/점(.)을 폴더로 쓰지 않도록 모두 '_' 처리
        s = s.replace('\\', '_').replace('/', '_');
        s = s.replace('.', '_');

        // 드라이브 문자(C:) 같은 ':' 제거
        s = s.replaceAll("(?i)^([a-z]):", "$1");

        s = s.replaceAll("[:*?\"<>|]", "_");
        s = s.replaceAll("\\s+", "_");
        s = s.replaceAll("_+", "_");

        if (s.length() > 180) s = s.substring(0, 180);
        return s.isBlank() ? "_" : s;
    }

    // -----------------------
    // LEGACY SAFE PATH (기존 규칙 호환)
    // -----------------------
    private static String toLegacySafePath(String raw, boolean allowSlash) {
        if (raw == null || raw.isBlank()) return "_";
        String s = raw.trim();

        if (s.startsWith("file:/")) s = s.substring("file:/".length());
        else if (s.startsWith("file:")) s = s.substring("file:".length());

        s = s.replace('\\', '/');
        s = s.replace('.', '/');

        // Remove ':' from drive prefix like C:
        s = s.replaceAll("(?i)^([a-z]):", "$1");

        s = s.replaceAll("[:*?\"<>|]", "_");
        s = s.replaceAll("\\s+", "_");
        s = s.replaceAll("/+", "/");

        if (!allowSlash) s = s.replace("/", "_");

        if (s.length() > 240) s = s.substring(0, 240);
        return s.isBlank() ? "_" : s;
    }

    private static String lastSegment(String s) {
        if (s == null) return "";
        String t = s.trim();
        int dot = t.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < t.length()) t = t.substring(dot + 1);
        return t;
    }

    private static String stripSuffix(String s, String suffix) {
        if (s == null) return "";
        if (suffix == null || suffix.isEmpty()) return s;
        if (s.length() >= suffix.length() && s.endsWith(suffix)) {
            return s.substring(0, s.length() - suffix.length());
        }
        return s;
    }
}
