package com.trayecto.analytics.application.query;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.api.TripsPublicApi;
import com.trayecto.trips.api.dto.TripSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agrega KM por mes a partir de los viajes COMPLETED del año pedido. Lee directo
 * vía {@link TripsPublicApi}, sin pasar por read-models materializados.
 */
@Component
public class GetKmByMonthHandler {

    private final TripsPublicApi tripsApi;

    public GetKmByMonthHandler(TripsPublicApi tripsApi) {
        this.tripsApi = tripsApi;
    }

    public record Query(UserId userId, int year) {}

    public record MonthlyKmPoint(String yearMonth, BigDecimal km) {}

    @Transactional(readOnly = true)
    public List<MonthlyKmPoint> handle(Query query) {
        Instant from = LocalDate.of(query.year(), 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = LocalDate.of(query.year() + 1, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Map<YearMonth, BigDecimal> byMonth = new HashMap<>();
        for (TripSnapshot t : tripsApi.listCompletedByUser(query.userId(), from, to)) {
            if (t.completedAt() == null || t.distanceKm() == null) continue;
            YearMonth ym = YearMonth.from(t.completedAt().atZone(ZoneOffset.UTC));
            byMonth.merge(ym, t.distanceKm(), BigDecimal::add);
        }

        // Devolvemos solo los meses con data, ordenados ascendente.
        // El frontend (Recharts) puede rellenar huecos si lo necesita.
        List<MonthlyKmPoint> points = new ArrayList<>();
        byMonth.keySet().stream().sorted().forEach(ym ->
            points.add(new MonthlyKmPoint(ym.toString(), byMonth.get(ym)))
        );
        return points;
    }
}
