package oasys.migration.alias;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class AliasSqlFileWriter {

    public void writeNewXml(Path originalXmlPath, String transformedXmlContent) throws IOException {
        // example: sql-aepafu001.xml -> sql-aepafu001-new.xml
        String originalName = originalXmlPath.getFileName().toString();
        String newName = originalName.replace(".xml", "-new.xml");
        Path newXmlPath = originalXmlPath.resolveSibling(newName);

        Files.writeString(
                newXmlPath,
                transformedXmlContent,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    public void write(
            Path outputRoot,
            String serviceClass,
            String mapperNamespace,
            String sqlId,
            String sqlContent
    ) throws IOException {

        if (outputRoot == null) throw new IllegalArgumentException("outputRoot is null");
        if (sqlId == null || sqlId.isBlank()) throw new IllegalArgumentException("sqlId is blank");

        String servicePath = toSafePath(serviceClass, true);
        String namespacePath = toSafePath(mapperNamespace, true);

        Path dir = outputRoot.resolve(servicePath).resolve(namespacePath);
        Files.createDirectories(dir);

        String safeFileName = toSafePath(sqlId, false) + ".sql";
        Path outFile = dir.resolve(safeFileName);

        Files.writeString(
                outFile,
                sqlContent == null ? "" : sqlContent,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private static String toSafePath(String raw, boolean allowSlash) {
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

        return s.isBlank() ? "_" : s;
    }
}
