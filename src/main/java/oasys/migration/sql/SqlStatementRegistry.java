package oasys.migration.sql;

public class SqlStatementRegistry {

    private final MapperXmlIndex index = new MapperXmlIndex();
    private boolean initialized = false;

    public void initialize() {
        if (initialized) return;

        SqlsDirectoryScanner scanner = new SqlsDirectoryScanner();
        index.buildIndex(scanner.scanAllSqlXml());

        initialized = true;
        System.out.println("[INIT] SQL index size = " + index.size());
    }

    public SqlStatement get(String namespace, String sqlId) {
        return index.get(namespace, sqlId);
    }
}
