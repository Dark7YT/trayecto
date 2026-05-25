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
 * Agrega gasto por mes a partir de los viajes COMPLETED del año pedido.
 */
@Component
public class GetCostByMonthHandler {

    private final TripsPublicApi tripsApi;

    public GetCostByMonthHandler(TripsPublicApi tripsApi) {
        this.tripsApi = tripsApi;
    }

    public record Query(UserId userId, int year) {}

    public record MonthlyCostPoint(String yearMonth, BigDecimal amount, String currency) {}

    @Transactional(readOnly = true)
    public List<MonthlyCostPoint> handle(Query query) {
        Instant from = LocalDate.of(query.year(), 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = LocalDate.of(query.year() + 1, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Map<YearMonth, BigDecimal> byMonth = new HashMap<>();
        String currency = "PEN";
        for (TripSnapshot t : tripsApi.listCompletedByUser(query.userId(), from, to)) {
            if (t.completedAt() == null || t.totalCost() == null) continue;
            YearMonth ym = YearMonth.from(t.completedAt().atZone(ZoneOffset.UTC));
            byMonth.merge(ym, t.totalCost().amount(), BigDecimal::add);
            currency = t.totalCost().currency().getCurrencyCode();
        }

        List<MonthlyCostPoint> points = new ArrayList<>();
        String currencyFinal = currency;
        byMonth.keySet().stream().sorted().forEach(ym ->
            points.add(new MonthlyCostPoint(ym.toString(), byMonth.get(ym), currencyFinal))
        );
        return points;
    }
}
