package com.trayecto.analytics.infrastructure.reports;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.trayecto.analytics.application.ReportFormatPort;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Adapter PDF basado en OpenPDF (fork libre de iText 4, sin AGPL).
 *
 * Diseño portfolio-grade: encabezado de marca con tipografía bold en color
 * indigo, metadata en dos columnas (usuario / período), tabla con header en
 * fondo indigo, filas alternadas en violet-wash muy claro, bloque de totales
 * con fondo SURFACE y valores destacados en indigo, pie con timestamp.
 *
 * Paleta sincronizada con globals.css del frontend (indigo primary chart-1).
 */
class OpenPdfReportAdapter {

    private static final Color PRIMARY     = new Color(91, 91, 214);   // indigo (chart-1)
    private static final Color SURFACE     = new Color(248, 247, 254); // violet wash claro
    private static final Color SURFACE_ALT = new Color(252, 251, 255); // alternancia muy sutil
    private static final Color BORDER      = new Color(228, 225, 240);
    private static final Color MUTED       = new Color(110, 113, 145);
    private static final Color HEADING     = new Color(33, 35, 64);    // foreground

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.of("America/Lima"));

    byte[] generate(ReportFormatPort.ReportData data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 60, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            Font brandFont    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PRIMARY);
            Font tagFont      = FontFactory.getFont(FontFactory.HELVETICA, 10, MUTED);
            Font labelFont    = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
            Font valueFont    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, HEADING);
            Font headerFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font cellFont     = FontFactory.getFont(FontFactory.HELVETICA, 9, HEADING);
            Font cellSoftFont = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED);
            Font totalLabel   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, HEADING);
            Font totalValue   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARY);
            Font footerFont   = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, MUTED);

            // ------- Encabezado de marca -------
            Paragraph brand = new Paragraph("Trayecto", brandFont);
            brand.setSpacingAfter(2f);
            document.add(brand);

            Paragraph tag = new Paragraph("Reporte de viajes", tagFont);
            tag.setSpacingAfter(16f);
            document.add(tag);

            // ------- Metadata: Usuario y Período en dos columnas -------
            PdfPTable meta = new PdfPTable(2);
            meta.setWidthPercentage(100);
            meta.setSpacingAfter(18f);
            meta.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            meta.addCell(metaCell("USUARIO", data.userDisplayName(), labelFont, valueFont));
            meta.addCell(metaCell("PERÍODO", data.periodLabel(), labelFont, valueFont));
            document.add(meta);

            // ------- Tabla de viajes -------
            PdfPTable table = new PdfPTable(new float[]{3, 1.4f, 1.4f, 1.2f, 1.2f, 1.6f, 2});
            table.setWidthPercentage(100);

            String[] headers = {
                "Viaje", "KM inicial", "KM final", "Distancia",
                "Multiplicador", "Costo", "Cerrado"
            };
            for (String h : headers) {
                PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
                c.setBackgroundColor(PRIMARY);
                c.setBorderColor(PRIMARY);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                c.setVerticalAlignment(Element.ALIGN_MIDDLE);
                c.setPadding(7f);
                c.setBorderWidth(0.5f);
                table.addCell(c);
            }

            int row = 0;
            for (var line : data.trips()) {
                Color bg = (row % 2 == 0) ? SURFACE_ALT : Color.WHITE;
                table.addCell(bodyCell(safe(line.name()), cellFont, Element.ALIGN_LEFT, bg));
                table.addCell(bodyCell(format(line.startKm()), cellFont, Element.ALIGN_RIGHT, bg));
                table.addCell(bodyCell(format(line.endKm()), cellFont, Element.ALIGN_RIGHT, bg));
                table.addCell(bodyCell(format(line.distanceKm()), cellFont, Element.ALIGN_RIGHT, bg));
                table.addCell(bodyCell("× " + format(line.multiplierRate()), cellFont,
                    Element.ALIGN_CENTER, bg));
                table.addCell(bodyCell(
                    String.format(Locale.US, "%s %s", data.totalCostCurrency(), formatMoney(line.costAmount())),
                    cellFont, Element.ALIGN_RIGHT, bg));
                table.addCell(bodyCell(
                    line.completedAt() != null ? DATE_FMT.format(line.completedAt()) : "—",
                    cellSoftFont, Element.ALIGN_CENTER, bg));
                row++;
            }

            if (row == 0) {
                PdfPCell empty = new PdfPCell(new Phrase(
                    "No hay viajes cerrados en este período.", cellSoftFont));
                empty.setColspan(headers.length);
                empty.setHorizontalAlignment(Element.ALIGN_CENTER);
                empty.setPadding(20f);
                empty.setBackgroundColor(SURFACE_ALT);
                empty.setBorderColor(BORDER);
                table.addCell(empty);
            }

            document.add(table);

            // ------- Bloque de totales -------
            document.add(Chunk.NEWLINE);
            PdfPTable totals = new PdfPTable(2);
            totals.setWidthPercentage(60);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            totals.addCell(totalsLabelCell("TOTAL KM", totalLabel));
            totals.addCell(totalsValueCell(format(data.totalKm()) + " km", totalValue));

            totals.addCell(totalsLabelCell("TOTAL A PAGAR", totalLabel));
            totals.addCell(totalsValueCell(
                String.format(Locale.US, "%s %s",
                    data.totalCostCurrency(), formatMoney(data.totalCostAmount())),
                totalValue
            ));
            document.add(totals);

            // ------- Pie -------
            document.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph(
                String.format(Locale.US,
                    "Generado por Trayecto · %s",
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                        .withZone(ZoneId.of("America/Lima"))
                        .format(Instant.now())),
                footerFont
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new BusinessRuleViolation("reports.pdf_failed", "Failed to generate PDF: " + e.getMessage());
        }
    }

    private static PdfPCell metaCell(String label, String value, Font labelFont, Font valueFont) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(2f);
        Paragraph p = new Paragraph();
        p.add(new Chunk(label, labelFont));
        p.add(Chunk.NEWLINE);
        p.add(new Chunk(value != null ? value : "—", valueFont));
        c.addElement(p);
        return c;
    }

    private static PdfPCell bodyCell(String text, Font font, int align, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(6f);
        c.setBackgroundColor(bg);
        c.setBorderColor(BORDER);
        c.setBorderWidth(0.5f);
        return c;
    }

    private static PdfPCell totalsLabelCell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setBackgroundColor(SURFACE);
        c.setPadding(8f);
        return c;
    }

    private static PdfPCell totalsValueCell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setBackgroundColor(SURFACE);
        c.setPadding(8f);
        return c;
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static String format(BigDecimal n) {
        if (n == null) return "—";
        return String.format(Locale.US, "%.1f", n);
    }

    private static String formatMoney(BigDecimal n) {
        if (n == null) return "—";
        return String.format(Locale.US, "%.2f", n);
    }

}
