package com.trayecto.analytics.application.query;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.api.TripsPublicApi;
import com.trayecto.trips.api.dto.TripSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * Calcula KPIs del dashboard agregando en memoria los viajes COMPLETED reales del
 * usuario obtenidos vía {@link TripsPublicApi}. No usa read-models materializados:
 * la "fuente única de verdad" es la tabla de viajes, así los números siempre
 * reflejan exactamente lo que el usuario tiene cerrado en este instante (incluso
 * tras borrados o ediciones), sin necesidad de un rebuild manual.
 */
@Component
public class GetDashboardHandler {

    private final TripsPublicApi tripsApi;

    public GetDashboardHandler(TripsPublicApi tripsApi) {
        this.tripsApi = tripsApi;
    }

    public record Query(UserId userId) {}

    public record DashboardSnapshot(
        BigDecimal totalKm,
        BigDecimal totalCostAmount,
        String totalCostCurrency,
        BigDecimal currentMonthCostAmount,
        BigDecimal currentMonthKm,
        int totalCompletedTrips,
        int currentMonthTrips
    ) {}

    @Transactional(readOnly = true)
    public DashboardSnapshot handle(Query query) {
        var trips = tripsApi.listCompletedByUser(query.userId(), null, null);
        YearMonth currentMonth = YearMonth.from(LocalDate.now(ZoneOffset.UTC));

        BigDecimal totalKm = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal currentMonthKm = BigDecimal.ZERO;
        BigDecimal currentMonthCost = BigDecimal.ZERO;
        int totalCompletedTrips = 0;
        int currentMonthTrips = 0;
        String currency = "PEN";

        for (TripSnapshot t : trips) {
            // Defensa contra datos incompletos: si falta cualquier campo agregable
            // saltamos el viaje en lugar de propagar NPE — un viaje COMPLETED
            // siempre debería tenerlos pero no asumimos invariantes externos.
            if (t.distanceKm() == null || t.totalCost() == null || t.completedAt() == null) {
                continue;
            }
            BigDecimal km = t.distanceKm();
            BigDecimal cost = t.totalCost().amount();
            totalKm = totalKm.add(km);
            totalCost = totalCost.add(cost);
            totalCompletedTrips++;
            currency = t.totalCost().currency().getCurrencyCode();

            YearMonth ym = YearMonth.from(t.completedAt().atZone(ZoneOffset.UTC));
            if (ym.equals(currentMonth)) {
                currentMonthKm = currentMonthKm.add(km);
                currentMonthCost = currentMonthCost.add(cost);
                currentMonthTrips++;
            }
        }

        return new DashboardSnapshot(
            totalKm,
            totalCost,
            currency,
            currentMonthCost,
            currentMonthKm,
            totalCompletedTrips,
            currentMonthTrips
        );
    }
}
