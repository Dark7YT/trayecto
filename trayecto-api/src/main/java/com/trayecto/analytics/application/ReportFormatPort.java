package com.trayecto.analytics.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Port de generación de reportes. Los adapters concretos (OpenPDF, Apache POI)
 * viven en {@code infrastructure.reports}. Los handlers de aplicación arman
 * {@link ReportData} y delegan acá — no conocen la librería subyacente.
 *
 * El reporte puede ser mensual ("Mayo 2026") o una selección arbitraria
 * ("Selección de 5 viajes"); ambas usan la misma estructura — solo cambian
 * los campos {@code periodLabel} y {@code sheetName} que el adapter pinta.
 */
public interface ReportFormatPort {

    byte[] generateReportPdf(ReportData data);

    byte[] generateReportXlsx(ReportData data);

    record ReportData(
        String userDisplayName,
        /** Etiqueta humana del período: "Mayo 2026" o "Selección personalizada (3 viajes)". */
        String periodLabel,
        /** Slug usado como nombre del sheet en Excel: "2026-05" o "seleccion". */
        String sheetName,
        List<TripLine> trips,
        BigDecimal totalKm,
        BigDecimal totalCostAmount,
        String totalCostCurrency
    ) {
        public record TripLine(
            String name,
            BigDecimal startKm,
            BigDecimal endKm,
            BigDecimal distanceKm,
            BigDecimal multiplierRate,
            BigDecimal costAmount,
            Instant completedAt
        ) {}
    }
}
