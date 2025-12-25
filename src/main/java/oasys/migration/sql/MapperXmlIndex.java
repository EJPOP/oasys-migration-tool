package oasys.migration.sql;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapperXmlIndex {

    private final Map<String, SqlStatement> index = new HashMap<>();
    private final MapperXmlLoader loader = new MapperXmlLoader();

    /**
     * namespace.sqlId → SqlStatement
     */
    public void buildIndex(List<Path> xmlFiles) {

        for (Path xml : xmlFiles) {
            Map<String, SqlStatement> parsed =
                    loader.loadFromFile(xml);

            for (SqlStatement stmt : parsed.values()) {
                String key = stmt.getNamespace() + "." + stmt.getSqlId();

                // 중복 발생 시 최초 1건 유지 (현행 시스템 기준)
                index.putIfAbsent(key, stmt);
            }
        }
    }

    public SqlStatement get(String namespace, String sqlId) {
        return index.get(namespace + "." + sqlId);
    }

    public int size() {
        return index.size();
    }
}
