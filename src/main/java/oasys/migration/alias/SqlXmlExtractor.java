package oasys.migration.alias;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class SqlXmlExtractor {

    public static class SqlXml {
        private final Path xmlPath;
        private final String xmlContent;

        public SqlXml(Path xmlPath, String xmlContent) {
            this.xmlPath = xmlPath;
            this.xmlContent = xmlContent;
        }

        public Path getXmlPath() {
            return xmlPath;
        }

        public String getXmlContent() {
            return xmlContent;
        }
    }

    public List<SqlXml> extractAll() throws Exception {

        List<SqlXml> list = new ArrayList<>();
        Path root = Paths.get("src/main/resources/sqls");

        Files.walk(root)
                .filter(p -> p.toString().endsWith(".xml"))
                .filter(p -> !p.toString().endsWith("-new.xml"))
                .forEach(p -> {
                    try {
                        list.add(
                                new SqlXml(
                                        p,
                                        Files.readString(p)
                                )
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        return list;
    }
}
