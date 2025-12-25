package oasys.migration.callchain;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ServiceSqlXrefLoader {

    private static final String[] REQUIRED_HEADERS = {
            "service",
            "service_method",
            "file",
            "namespace",
            "id"
    };

    public List<ServiceSqlCall> load(String classpathLocation) {

        try (
                InputStream is = openStream(classpathLocation);
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                CSVParser parser =
                        CSVFormat.DEFAULT
                                .withFirstRecordAsHeader()
                                .withTrim()
                                .parse(reader)
        ) {

            validateHeaders(parser);

            List<ServiceSqlCall> result = new ArrayList<>();

            for (CSVRecord record : parser) {

                String service = record.get("service");
                String serviceMethod = record.get("service_method");
                String mapperFile = record.get("file");
                String namespace = record.get("namespace");
                String sqlId = record.get("id");

                // optional columns (newer xref)
                String tag = getOptional(record, parser, "tag");
                String sqlText = getOptional(record, parser, "sql_text");

                if (isBlank(service) || isBlank(serviceMethod) || isBlank(sqlId)) {
                    continue;
                }

                if (tag != null || sqlText != null) {
                    result.add(
                            new ServiceSqlCall(
                                    service,
                                    serviceMethod,
                                    mapperFile,
                                    namespace,
                                    tag,
                                    sqlId,
                                    sqlText
                            )
                    );
                } else {
                    result.add(
                            new ServiceSqlCall(
                                    service,
                                    serviceMethod,
                                    mapperFile,
                                    namespace,
                                    sqlId
                            )
                    );
                }
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load service_sql_xref.csv", e);
        }
    }

    /**
     * 1) file:/... URI 또는 파일시스템 경로가 존재하면 그걸 사용
     * 2) 아니면 classpath 리소스로 로딩
     */
    private InputStream openStream(String location) throws Exception {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("location is blank");
        }

        String loc = location.trim();

        if (loc.startsWith("file:")) {
            Path p = Path.of(URI.create(loc));
            if (!Files.exists(p)) {
                throw new IllegalStateException("File not found: " + p);
            }
            return Files.newInputStream(p);
        }

        Path p = Path.of(loc);
        if (Files.exists(p)) {
            return Files.newInputStream(p);
        }

        InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(loc);

        if (is == null) {
            throw new IllegalStateException(
                    "service_sql_xref.csv not found: " + loc + " (classpath or filesystem)"
            );
        }
        return is;
    }

    private String getOptional(CSVRecord record, CSVParser parser, String name) {
        if (parser.getHeaderMap() != null && parser.getHeaderMap().containsKey(name)) {
            String v = record.get(name);
            return isBlank(v) ? null : v;
        }
        return null;
    }

    private void validateHeaders(CSVParser parser) {
        for (String header : REQUIRED_HEADERS) {
            if (!parser.getHeaderMap().containsKey(header)) {
                throw new IllegalStateException(
                        "Required header missing in service_sql_xref.csv: " + header
                );
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
