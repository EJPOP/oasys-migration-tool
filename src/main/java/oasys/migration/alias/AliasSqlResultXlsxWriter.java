package oasys.migration.alias;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class AliasSqlResultXlsxWriter {

    /**
     * 결과 XLSX를 스트리밍 방식으로 작성한다.
     * - autoSizeColumn()은 대량 데이터에서 병목/멈춤 원인이 될 수 있으므로 사용하지 않는다.
     */
    public void write(Path outputFile, List<AliasSqlResult> results) {

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
