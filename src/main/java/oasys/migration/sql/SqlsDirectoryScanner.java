package oasys.migration.sql;

import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class SqlsDirectoryScanner {

    /**
     * classpath 상의 sqls 디렉터리 전체 XML 파일 스캔
     */
    public List<Path> scanAllSqlXml() {

        List<Path> result = new ArrayList<>();

        try {
            URL url = Thread.currentThread()
                    .getContextClassLoader()
                    .getResource("sqls");

            if (url == null) {
                throw new IllegalStateException("sqls directory not found in classpath");
            }

            Path root = Paths.get(url.toURI());

            Files.walk(root)
                    .filter(p -> Files.isRegularFile(p))
                    .filter(p -> p.toString().endsWith(".xml"))
                    .forEach(result::add);

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to scan sqls directory", e);
        }
    }
}
