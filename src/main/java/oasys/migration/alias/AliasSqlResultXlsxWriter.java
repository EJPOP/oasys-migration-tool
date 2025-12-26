package oasys.migration.alias;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

public class AliasSqlResultXlsxWriter {

    /* ===== CHANGED START ===== */
    public static final class TobeSelectOutputRow {
        public final String serviceClass;
        public final String namespace;
        public final String sqlId;
        public final int seq;
        public final String outputName;
        public final String lowerCamel;
        public final String expr;
        public final String comment;

        public TobeSelectOutputRow(String serviceClass, String namespace, String sqlId, int seq,
                                   String outputName, String lowerCamel, String expr, String comment) {
            this.serviceClass = serviceClass;
            this.namespace = namespace;
            this.sqlId = sqlId;
            this.seq = seq;
            this.outputName = outputName;
            this.lowerCamel = lowerCamel;
            this.expr = expr;
            this.comment = comment;
        }
    }

    public static final class TobeDmlParamRow {
        public final String serviceClass;
        public final String namespace;
        public final String sqlId;
        public final int seq;
        public final String dmlType;
        public final String paramName;
        public final String lowerCamel;

        public TobeDmlParamRow(String serviceClass, String namespace, String sqlId, int seq,
                               String dmlType, String paramName, String lowerCamel) {
            this.serviceClass = serviceClass;
            this.namespace = namespace;
            this.sqlId = sqlId;
            this.seq = seq;
            this.dmlType = dmlType;
            this.paramName = paramName;
            this.lowerCamel = lowerCamel;
        }
    }
    /* ===== CHANGED END ===== */

    /**
     * 결과 XLSX를 스트리밍 방식으로 작성한다.
     * - autoSizeColumn()은 대량 데이터에서 병목/멈춤 원인이 될 수 있으므로 사용하지 않는다.
     */
    public void write(Path outputFile, List<AliasSqlResult> results) {
        /* ===== CHANGED START ===== */
        write(outputFile, results, Collections.emptyList(), Collections.emptyList());
        /* ===== CHANGED END ===== */
    }

    /* ===== CHANGED START ===== */
    public void write(Path outputFile,
                      List<AliasSqlResult> results,
                      List<TobeSelectOutputRow> tobeSelectOutputs,
                      List<TobeDmlParamRow> tobeDmlParams) {
        /* ===== CHANGED END ===== */

        try {
            if (outputFile.getParent() != null) {
                Files.createDirectories(outputFile.getParent());
            }

            // rowAccessWindowSize: 메모리에 유지할 행 수(너무 크게 잡을 필요 없음)
            // compressTempFiles: 임시 파일 압축(디스크 사용량 감소)
            SXSSFWorkbook wb = new SXSSFWorkbook(200);
            wb.setCompressTempFiles(true);

            try (wb) {
                Sheet sheet = wb.createSheet("alias-sql-result");

                // Styles (한 번만 생성해서 재사용)
                CellStyle headerStyle = createHeaderStyle(wb);
                CellStyle bodyStyle = createBodyStyle(wb);

                // Header
                Row header = sheet.createRow(0);
                createCell(header, 0, "결과", headerStyle);
                createCell(header, 1, "서비스클래스", headerStyle);
                createCell(header, 2, "Mapper Namespace", headerStyle);
                createCell(header, 3, "SQL ID", headerStyle);
                createCell(header, 4, "사유", headerStyle);

                int rowIdx = 1;
                for (AliasSqlResult r : results) {
                    Row row = sheet.createRow(rowIdx++);
                    createCell(row, 0, safe(r.result), bodyStyle);
                    createCell(row, 1, safe(r.serviceClass), bodyStyle);
                    createCell(row, 2, safe(r.namespace), bodyStyle);
                    createCell(row, 3, safe(r.sqlId), bodyStyle);
                    createCell(row, 4, safe(r.reason), bodyStyle);
                }

                // ❌ autoSizeColumn 제거(성능 병목)
                // ✅ 고정 폭 설정(문자수 기준 * 256)
                sheet.setColumnWidth(0, 10 * 256);  // 결과
                sheet.setColumnWidth(1, 50 * 256);  // 서비스클래스
                sheet.setColumnWidth(2, 60 * 256);  // namespace
                sheet.setColumnWidth(3, 35 * 256);  // sqlId
                sheet.setColumnWidth(4, 50 * 256);  // 사유

                /* ===== CHANGED START ===== */
                if (tobeSelectOutputs != null && !tobeSelectOutputs.isEmpty()) {
                    Sheet s2 = wb.createSheet("tobe-select-outputs");

                    Row h = s2.createRow(0);
                    createCell(h, 0, "서비스클래스", headerStyle);
                    createCell(h, 1, "Mapper Namespace", headerStyle);
                    createCell(h, 2, "SQL ID", headerStyle);
                    createCell(h, 3, "순번", headerStyle);
                    createCell(h, 4, "출력명", headerStyle);
                    createCell(h, 5, "lowerCamel", headerStyle);
                    createCell(h, 6, "expr", headerStyle);
                    createCell(h, 7, "comment", headerStyle);

                    int r = 1;
                    for (TobeSelectOutputRow x : tobeSelectOutputs) {
                        Row row = s2.createRow(r++);
                        createCell(row, 0, safe(x.serviceClass), bodyStyle);
                        createCell(row, 1, safe(x.namespace), bodyStyle);
                        createCell(row, 2, safe(x.sqlId), bodyStyle);
                        createCell(row, 3, String.valueOf(x.seq), bodyStyle);
                        createCell(row, 4, safe(x.outputName), bodyStyle);
                        createCell(row, 5, safe(x.lowerCamel), bodyStyle);
                        createCell(row, 6, safe(x.expr), bodyStyle);
                        createCell(row, 7, safe(x.comment), bodyStyle);
                    }

                    s2.setColumnWidth(0, 50 * 256);
                    s2.setColumnWidth(1, 60 * 256);
                    s2.setColumnWidth(2, 35 * 256);
                    s2.setColumnWidth(3, 8 * 256);
                    s2.setColumnWidth(4, 35 * 256);
                    s2.setColumnWidth(5, 35 * 256);
                    s2.setColumnWidth(6, 90 * 256);
                    s2.setColumnWidth(7, 50 * 256);
                }

                if (tobeDmlParams != null && !tobeDmlParams.isEmpty()) {
                    Sheet s3 = wb.createSheet("tobe-dml-params");

                    Row h = s3.createRow(0);
                    createCell(h, 0, "서비스클래스", headerStyle);
                    createCell(h, 1, "Mapper Namespace", headerStyle);
                    createCell(h, 2, "SQL ID", headerStyle);
                    createCell(h, 3, "순번", headerStyle);
                    createCell(h, 4, "DML", headerStyle);
                    createCell(h, 5, "paramName", headerStyle);
                    createCell(h, 6, "lowerCamel", headerStyle);

                    int r = 1;
                    for (TobeDmlParamRow x : tobeDmlParams) {
                        Row row = s3.createRow(r++);
                        createCell(row, 0, safe(x.serviceClass), bodyStyle);
                        createCell(row, 1, safe(x.namespace), bodyStyle);
                        createCell(row, 2, safe(x.sqlId), bodyStyle);
                        createCell(row, 3, String.valueOf(x.seq), bodyStyle);
                        createCell(row, 4, safe(x.dmlType), bodyStyle);
                        createCell(row, 5, safe(x.paramName), bodyStyle);
                        createCell(row, 6, safe(x.lowerCamel), bodyStyle);
                    }

                    s3.setColumnWidth(0, 50 * 256);
                    s3.setColumnWidth(1, 60 * 256);
                    s3.setColumnWidth(2, 35 * 256);
                    s3.setColumnWidth(3, 8 * 256);
                    s3.setColumnWidth(4, 10 * 256);
                    s3.setColumnWidth(5, 35 * 256);
                    s3.setColumnWidth(6, 35 * 256);
                }
                /* ===== CHANGED END ===== */

                try (OutputStream os = Files.newOutputStream(
                        outputFile,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                )) {
                    wb.write(os);
                }

                // SXSSF 임시파일 제거(try-with-resources로 close는 되지만 dispose를 명시적으로 호출해도 안전)
                wb.dispose();
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to write alias SQL result XLSX: " + outputFile, e);
        }
    }

    private static void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        if (style != null) cell.setCellStyle(style);
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private static CellStyle createBodyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }
}
