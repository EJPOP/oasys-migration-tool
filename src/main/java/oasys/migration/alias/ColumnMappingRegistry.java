package oasys.migration.alias;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ColumnMappingRegistry {

    // 원본 매핑: key = ASIS_TABLE.ASIS_COLUMN
    private final Map<String, ColumnMapping> mappingMap;

    // 보조 인덱스(컬럼)
    private final Map<String, ColumnMapping> byAsisColOnly = new HashMap<>();     // key=ASIS_COL
    private final Map<String, ColumnMapping> byTobeColOnly = new HashMap<>();     // key=TOBE_COL
    private final Map<String, ColumnMapping> byTobeOnAsisTable = new HashMap<>(); // key=ASIS_TABLE.TOBE_COL

    // ✅ 테이블 매핑(현행테이블ID -> (TOBE)테이블ID)
    private final Map<String, String> asisToTobeTableId = new HashMap<>();

    public ColumnMappingRegistry(String xlsxResourcePath) {

        String resolvedPath = resolveMappingXlsxPath(xlsxResourcePath);

        ColumnMappingXlsxLoader loader = new ColumnMappingXlsxLoader(); // no-arg constructor
        this.mappingMap = loader.load(resolvedPath);                    // load(String)

        // 인덱스 구성
        for (ColumnMapping m : mappingMap.values()) {
            String asisTable = upper(m.asisTableId);
            String asisCol = upper(m.asisColumnId);
            String tobeCol = upper(m.tobeColumnId);

            byAsisColOnly.putIfAbsent(asisCol, m);
            byTobeColOnly.putIfAbsent(tobeCol, m);
            byTobeOnAsisTable.put(asisTable + "." + tobeCol, m);

            // ✅ (현행테이블ID -> 테이블ID) 매핑 수집
            // ColumnMapping 필드/Getter 명이 프로젝트마다 다를 수 있어 reflection으로 읽음
            String tobeTable = upper(readAnyString(m,
                    "tobeTableId", "tableId", "tobeTblId", "tobeTable",
                    "TOBE_TABLE_ID", "TABLE_ID"
            ));

            if (!asisTable.isEmpty() && !tobeTable.isEmpty()) {
                asisToTobeTableId.putIfAbsent(asisTable, tobeTable);
            }
        }
    }

    /** ✅ 로딩된 컬럼 매핑 개수 */
    public int size() {
        return mappingMap == null ? 0 : mappingMap.size();
    }

    /** 테이블 + 컬럼 기준 (ASIS_TABLE + ASIS_COLUMN) */
    public ColumnMapping find(String table, String column) {
        if (table == null || column == null) return null;
        return mappingMap.get(upper(table) + "." + upper(column));
    }

    /** 컬럼만 기준 (ASIS_COLUMN) */
    public ColumnMapping findByColumnOnly(String column) {
        if (column == null) return null;
        return byAsisColOnly.get(upper(column));
    }

    /** 컬럼만 기준 (TOBE_COLUMN) */
    public ColumnMapping findByTobeColumnOnly(String tobeColumnId) {
        if (tobeColumnId == null) return null;
        return byTobeColOnly.get(upper(tobeColumnId));
    }

    /**
     * 변환 과정에서 테이블은 아직 ASIS인데,
     * 컬럼은 이미 TOBE로 바뀐 상황을 커버하기 위한 역조회
     * (ASIS_TABLE + TOBE_COLUMN)
     */
    public ColumnMapping findByTobeOnAsisTable(String asisTableId, String tobeColumnId) {
        if (asisTableId == null || tobeColumnId == null) return null;
        return byTobeOnAsisTable.get(upper(asisTableId) + "." + upper(tobeColumnId));
    }

    /** ✅ TOBE_SQL 모드에서 테이블 치환용: 현행테이블ID -> (TOBE)테이블ID */
    public String findTobeTableId(String asisTableId) {
        if (asisTableId == null) return null;
        return asisToTobeTableId.get(upper(asisTableId));
    }

    private String upper(String s) {
        return (s == null) ? "" : s.trim().toUpperCase();
    }

    /**
     * ColumnMapping 객체에서 필드명/Getter명이 어떤 형태든 문자열을 최대한 찾아낸다.
     */
    private static String readAnyString(Object obj, String... candidates) {
        if (obj == null || candidates == null) return null;

        Class<?> c = obj.getClass();

        for (String name : candidates) {
            // 1) getter: getXxx()
            String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            try {
                Method m = c.getMethod(getter);
                Object v = m.invoke(obj);
                if (v != null) return String.valueOf(v);
            } catch (Exception ignored) {}

            // 2) field direct
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                Object v = f.get(obj);
                if (v != null) return String.valueOf(v);
            } catch (Exception ignored) {}
        }

        return null;
    }

    // ============================================================
    // ✅ jar 위치(baseDir) 기준: column_mapping.xlsx 경로 해석
    // ============================================================

    private static String resolveMappingXlsxPath(String raw) {

        Path baseDir = resolveBaseDir();

        // 값이 없으면 기본 후보를 순서대로 탐색
        if (raw == null || raw.isBlank()) {
            Path p1 = baseDir.resolve("column_mapping.xlsx");
            if (Files.exists(p1)) return p1.toAbsolutePath().normalize().toString();

            Path p2 = baseDir.resolve("mapping").resolve("column_mapping.xlsx");
            if (Files.exists(p2)) return p2.toAbsolutePath().normalize().toString();

            return p2.toAbsolutePath().normalize().toString();
        }

        // 값이 있으면: 상대경로는 baseDir 기준
        Path p = Path.of(raw.trim());
        if (!p.isAbsolute()) p = baseDir.resolve(p);
        p = p.toAbsolutePath().normalize();

        // 지정 경로가 없으면 baseDir 루트 파일도 한 번 더 탐색 (호환성)
        if (!Files.exists(p)) {
            Path alt = baseDir.resolve("column_mapping.xlsx");
            if (Files.exists(alt)) return alt.toAbsolutePath().normalize().toString();
        }

        return p.toString();
    }

    private static Path resolveBaseDir() {

        // 1) explicit override
        String baseDir = System.getProperty("oasys.migration.baseDir");
        if (baseDir != null && !baseDir.isBlank()) {
            try {
                return Path.of(baseDir).toAbsolutePath().normalize();
            } catch (Exception ignored) {
            }
        }

        // 2) jar location (when running as jar / library)
        try {
            var cs = ColumnMappingRegistry.class.getProtectionDomain().getCodeSource();
            if (cs != null && cs.getLocation() != null) {
                Path codePath = Path.of(cs.getLocation().toURI()).toAbsolutePath().normalize();
                String lower = codePath.toString().toLowerCase(Locale.ROOT);

                if (Files.isRegularFile(codePath) && lower.endsWith(".jar")) {
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
