package com.trayecto.analytics.application;

import com.trayecto.analytics.application.ReportFormatPort.ReportData;
import com.trayecto.analytics.application.ReportFormatPort.ReportData.TripLine;
import com.trayecto.iam.api.IamPublicApi;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.trips.api.TripsPublicApi;
import com.trayecto.trips.api.dto.TripSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Reporte mensual: agrupa viajes COMPLETED del usuario en un año/mes específico.
 *
 * Pipeline:
 * 1) Resuelve userDisplayName via {@link IamPublicApi#findUserSnapshot} (fallback "Usuario").
 * 2) Reúne viajes via {@link TripsPublicApi#listCompletedByUser} con rango [from, to).
 * 3) Suma totales en memoria.
 * 4) Delega al port {@link ReportFormatPort} con un {@link ReportData} que
 *    incluye {@code periodLabel} y {@code sheetName} ya formateados.
 */
@Component
public class GenerateMonthlyReportHandler {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final IamPublicApi iamApi;
    private final TripsPublicApi tripsApi;
    private final ReportFormatPort reportFormat;

    public GenerateMonthlyReportHandler(
        IamPublicApi iamApi,
        TripsPublicApi tripsApi,
        ReportFormatPort reportFormat
    ) {
        this.iamApi = iamApi;
        this.tripsApi = tripsApi;
        this.reportFormat = reportFormat;
    }

    public enum Format { PDF, XLSX }

    public record Command(UserId userId, int year, int month, Format format) {}

    @Transactional(readOnly = true)
    public byte[] handle(Command cmd) {
        if (cmd.month() < 1 || cmd.month() > 12) {
            throw new BusinessRuleViolation("reports.invalid_month", "Month must be 1..12");
        }
        if (cmd.year() < 2000 || cmd.year() > 2999) {
            throw new BusinessRuleViolation("reports.invalid_year", "Year out of range");
        }

        YearMonth ym = YearMonth.of(cmd.year(), cmd.month());
        ZonedDateTime startOfMonth = ym.atDay(1).atStartOfDay(LIMA);
        ZonedDateTime startOfNextMonth = ym.plusMonths(1).atDay(1).atStartOfDay(LIMA);
        Instant from = startOfMonth.toInstant();
        Instant toExclusive = startOfNextMonth.toInstant();

        String displayName = iamApi.findUserSnapshot(cmd.userId())
            .map(s -> s.displayName())
            .orElse("Usuario");

        List<TripSnapshot> trips = tripsApi.listCompletedByUser(cmd.userId(), from, toExclusive);

        String periodLabel = String.format("%s %d", monthName(cmd.month()), cmd.year());
        String sheetName = String.format("%04d-%02d", cmd.year(), cmd.month());

        ReportData data = buildReportData(displayName, periodLabel, sheetName, trips);
        return switch (cmd.format()) {
            case PDF -> reportFormat.generateReportPdf(data);
            case XLSX -> reportFormat.generateReportXlsx(data);
        };
    }

    /**
     * Construye el {@link ReportData} con los totales calculados sobre la lista
     * de viajes. Helper compartido — también lo usa {@code GenerateCustomReportHandler}
     * cuando el usuario exporta una selección arbitraria.
     */
    static ReportData buildReportData(
        String displayName,
        String periodLabel,
        String sheetName,
        List<TripSnapshot> trips
    ) {
        List<TripLine> lines = trips.stream().map(t -> new TripLine(
            t.name(),
            t.startKm(),
            t.endKm(),
            t.distanceKm(),
            t.multiplierRate(),
            t.totalCost() != null ? t.totalCost().amount() : BigDecimal.ZERO,
            t.completedAt()
        )).toList();

        BigDecimal totalKm = lines.stream()
            .map(l -> l.distanceKm() != null ? l.distanceKm() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCostAmount = lines.stream()
            .map(TripLine::costAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        String currency = trips.stream()
            .filter(t -> t.totalCost() != null)
            .findFirst()
            .map(t -> t.totalCost().currency().getCurrencyCode())
            .orElse("PEN");

        return new ReportData(displayName, periodLabel, sheetName,
            lines, totalKm, totalCostAmount, currency);
    }

    private static String monthName(int month) {
        return switch (month) {
            case 1 -> "Enero";
            case 2 -> "Febrero";
            case 3 -> "Marzo";
            case 4 -> "Abril";
            case 5 -> "Mayo";
            case 6 -> "Junio";
            case 7 -> "Julio";
            case 8 -> "Agosto";
            case 9 -> "Septiembre";
            case 10 -> "Octubre";
            case 11 -> "Noviembre";
            case 12 -> "Diciembre";
            default -> String.valueOf(month);
        };
    }
}
