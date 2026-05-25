package com.trayecto.analytics.infrastructure.reports;

import com.trayecto.analytics.application.ReportFormatPort;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Adapter Excel (xlsx) basado en Apache POI 5.x.
 *
 * Diseño portfolio-grade: encabezado de marca, metadata en grid, header de
 * tabla en indigo con texto blanco, alternancia de filas, freeze pane en el
 * header, formato monetario y KM por columna, totales con fondo SURFACE y
 * valor en indigo.
 *
 * Paleta sincronizada con el PDF y el frontend.
 */
class ApachePoiReportAdapter {

    private static final byte[] PRIMARY     = rgb(91, 91, 214);
    private static final byte[] PRIMARY_DK  = rgb(70, 70, 180);
    private static final byte[] SURFACE     = rgb(248, 247, 254);
    private static final byte[] SURFACE_ALT = rgb(252, 251, 255);
    private static final byte[] WHITE       = rgb(255, 255, 255);
    private static final byte[] BORDER      = rgb(228, 225, 240);
    private static final byte[] HEADING     = rgb(33, 35, 64);
    private static final byte[] MUTED       = rgb(110, 113, 145);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.of("America/Lima"));

    byte[] generate(ReportFormatPort.ReportData data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet(data.sheetName());

            // --- Estilos ---
            XSSFCellStyle brandStyle = workbook.createCellStyle();
            XSSFFont brandFont = workbook.createFont();
            brandFont.setBold(true);
            brandFont.setFontHeightInPoints((short) 18);
            brandFont.setColor(new XSSFColor(PRIMARY, null));
            brandStyle.setFont(brandFont);

            XSSFCellStyle subTitleStyle = workbook.createCellStyle();
            XSSFFont subTitleFont = workbook.createFont();
            subTitleFont.setFontHeightInPoints((short) 10);
            subTitleFont.setColor(new XSSFColor(MUTED, null));
            subTitleStyle.setFont(subTitleFont);

            XSSFCellStyle labelStyle = workbook.createCellStyle();
            XSSFFont labelFont = workbook.createFont();
            labelFont.setFontHeightInPoints((short) 9);
            labelFont.setColor(new XSSFColor(MUTED, null));
            labelStyle.setFont(labelFont);

            XSSFCellStyle valueStyle = workbook.createCellStyle();
            XSSFFont valueFont = workbook.createFont();
            valueFont.setBold(true);
            valueFont.setFontHeightInPoints((short) 11);
            valueFont.setColor(new XSSFColor(HEADING, null));
            valueStyle.setFont(valueFont);

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(new XSSFColor(WHITE, null));
            headerFont.setFontHeightInPoints((short) 10);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(PRIMARY, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            allBorders(headerStyle, BorderStyle.THIN, PRIMARY_DK);

            XSSFCellStyle rowStyleEven = bodyStyle(workbook, SURFACE_ALT);
            XSSFCellStyle rowStyleOdd  = bodyStyle(workbook, WHITE);

            XSSFCellStyle moneyEven = numericStyle(workbook, "#,##0.00", SURFACE_ALT);
            XSSFCellStyle moneyOdd  = numericStyle(workbook, "#,##0.00", WHITE);

            XSSFCellStyle kmEven = numericStyle(workbook, "#,##0.0", SURFACE_ALT);
            XSSFCellStyle kmOdd  = numericStyle(workbook, "#,##0.0", WHITE);

            XSSFCellStyle totalLabelStyle = workbook.createCellStyle();
            XSSFFont totalLabelFont = workbook.createFont();
            totalLabelFont.setBold(true);
            totalLabelFont.setFontHeightInPoints((short) 11);
            totalLabelFont.setColor(new XSSFColor(HEADING, null));
            totalLabelStyle.setFont(totalLabelFont);
            totalLabelStyle.setFillForegroundColor(new XSSFColor(SURFACE, null));
            totalLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalLabelStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFCellStyle totalCostValueStyle = workbook.createCellStyle();
            XSSFFont totalValueFont = workbook.createFont();
            totalValueFont.setBold(true);
            totalValueFont.setFontHeightInPoints((short) 14);
            totalValueFont.setColor(new XSSFColor(PRIMARY, null));
            totalCostValueStyle.setFont(totalValueFont);
            totalCostValueStyle.setFillForegroundColor(new XSSFColor(SURFACE, null));
            totalCostValueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalCostValueStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
            totalCostValueStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalCostValueStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFCellStyle totalKmValueStyle = workbook.createCellStyle();
            totalKmValueStyle.cloneStyleFrom(totalCostValueStyle);
            totalKmValueStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.0\" km\""));

            // --- Contenido ---
            int rowIdx = 0;
            Row brandRow = sheet.createRow(rowIdx++);
            brandRow.setHeightInPoints(28);
            Cell brandCell = brandRow.createCell(0);
            brandCell.setCellValue("Trayecto");
            brandCell.setCellStyle(brandStyle);

            Row subRow = sheet.createRow(rowIdx++);
            Cell subCell = subRow.createCell(0);
            subCell.setCellValue("Reporte de viajes");
            subCell.setCellStyle(subTitleStyle);

            rowIdx++; // espacio

            // Metadata Usuario / Período
            Row metaLabelRow = sheet.createRow(rowIdx++);
            metaCell(metaLabelRow, 0, "USUARIO", labelStyle);
            metaCell(metaLabelRow, 3, "PERÍODO", labelStyle);

            Row metaValueRow = sheet.createRow(rowIdx++);
            metaCell(metaValueRow, 0, data.userDisplayName(), valueStyle);
            metaCell(metaValueRow, 3, data.periodLabel(), valueStyle);

            rowIdx++; // espacio

            // Headers
            Row header = sheet.createRow(rowIdx);
            header.setHeightInPoints(22);
            String[] headers = {
                "Viaje", "KM inicial", "KM final", "Distancia (km)",
                "Multiplicador", "Costo (" + data.totalCostCurrency() + ")", "Cerrado"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            sheet.createFreezePane(0, rowIdx + 1);
            rowIdx++;

            int dataRowIdx = 0;
            for (var line : data.trips()) {
                boolean even = (dataRowIdx % 2 == 0);
                XSSFCellStyle txt = even ? rowStyleEven : rowStyleOdd;
                XSSFCellStyle money = even ? moneyEven : moneyOdd;
                XSSFCellStyle km = even ? kmEven : kmOdd;

                Row r = sheet.createRow(rowIdx++);

                Cell c0 = r.createCell(0);
                c0.setCellValue(safe(line.name()));
                c0.setCellStyle(txt);

                writeNumeric(r, 1, line.startKm(), km);
                writeNumeric(r, 2, line.endKm(), km);
                writeNumeric(r, 3, line.distanceKm(), km);

                Cell c4 = r.createCell(4);
                c4.setCellValue(line.multiplierRate() != null
                    ? "× " + line.multiplierRate().stripTrailingZeros().toPlainString()
                    : "—");
                c4.setCellStyle(txt);

                writeNumeric(r, 5, line.costAmount(), money);

                Cell c6 = r.createCell(6);
                c6.setCellValue(line.completedAt() != null
                    ? DATE_FMT.format(line.completedAt())
                    : "—");
                c6.setCellStyle(txt);

                dataRowIdx++;
            }

            if (dataRowIdx == 0) {
                Row emptyRow = sheet.createRow(rowIdx++);
                Cell e = emptyRow.createCell(0);
                e.setCellValue("No hay viajes cerrados en este período.");
                e.setCellStyle(rowStyleOdd);
                sheet.addMergedRegion(new CellRangeAddress(
                    emptyRow.getRowNum(), emptyRow.getRowNum(), 0, headers.length - 1));
            }

            rowIdx++; // espacio
            Row totalKmRow = sheet.createRow(rowIdx++);
            totalKmRow.setHeightInPoints(22);
            Cell tkLabel = totalKmRow.createCell(2);
            tkLabel.setCellValue("TOTAL KM");
            tkLabel.setCellStyle(totalLabelStyle);
            sheet.addMergedRegion(new CellRangeAddress(
                totalKmRow.getRowNum(), totalKmRow.getRowNum(), 0, 2));
            Cell tkValue = totalKmRow.createCell(3);
            tkValue.setCellValue(toDouble(data.totalKm()));
            tkValue.setCellStyle(totalKmValueStyle);

            Row totalCostRow = sheet.createRow(rowIdx++);
            totalCostRow.setHeightInPoints(22);
            Cell tcLabel = totalCostRow.createCell(2);
            tcLabel.setCellValue("TOTAL A PAGAR (" + data.totalCostCurrency() + ")");
            tcLabel.setCellStyle(totalLabelStyle);
            sheet.addMergedRegion(new CellRangeAddress(
                totalCostRow.getRowNum(), totalCostRow.getRowNum(), 0, 4));
            Cell tcValue = totalCostRow.createCell(5);
            tcValue.setCellValue(toDouble(data.totalCostAmount()));
            tcValue.setCellStyle(totalCostValueStyle);

            // Auto-size + mínimo decente para que se vea aireado.
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3500) sheet.setColumnWidth(i, 3500);
            }
            if (sheet.getColumnWidth(0) < 6000) sheet.setColumnWidth(0, 6000);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessRuleViolation("reports.xlsx_failed", "Failed to generate Excel: " + e.getMessage());
        }
    }

    // ------- helpers -------

    private static XSSFCellStyle bodyStyle(XSSFWorkbook wb, byte[] bg) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setColor(new XSSFColor(HEADING, null));
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(bg, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        allBorders(s, BorderStyle.HAIR, BORDER);
        return s;
    }

    private static XSSFCellStyle numericStyle(XSSFWorkbook wb, String fmt, byte[] bg) {
        XSSFCellStyle s = bodyStyle(wb, bg);
        s.setDataFormat(wb.createDataFormat().getFormat(fmt));
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private static void allBorders(XSSFCellStyle s, BorderStyle style, byte[] colorBytes) {
        XSSFColor c = new XSSFColor(colorBytes, null);
        s.setBorderTop(style);
        s.setBorderBottom(style);
        s.setBorderLeft(style);
        s.setBorderRight(style);
        s.setTopBorderColor(c);
        s.setBottomBorderColor(c);
        s.setLeftBorderColor(c);
        s.setRightBorderColor(c);
    }

    private static void metaCell(Row row, int col, String value, XSSFCellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "—");
        c.setCellStyle(style);
    }

    private static void writeNumeric(Row r, int col, BigDecimal value, XSSFCellStyle style) {
        Cell c = r.createCell(col);
        if (value != null) {
            c.setCellValue(value.doubleValue());
        }
        c.setCellStyle(style);
    }

    private static double toDouble(BigDecimal n) {
        return n != null ? n.doubleValue() : 0.0;
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static byte[] rgb(int r, int g, int b) {
        return new byte[]{(byte) r, (byte) g, (byte) b};
    }

}
