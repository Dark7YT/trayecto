package com.trayecto.analytics.application.query;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.api.TripsPublicApi;
import com.trayecto.trips.api.dto.MultiplierSnapshot;
import com.trayecto.trips.api.dto.TripSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agrupa los viajes COMPLETED del usuario por multiplicador y devuelve cuántas
 * veces usó cada uno con su porcentaje sobre el total. Resuelve nombre legible
 * vía {@link TripsPublicApi#listMultipliersByUser(UserId)}.
 */
@Component
public class GetMultiplierUsageHandler {

    private final TripsPublicApi tripsApi;

    public GetMultiplierUsageHandler(TripsPublicApi tripsApi) {
        this.tripsApi = tripsApi;
    }

    public record Query(UserId userId) {}

    public record MultiplierUsagePoint(
        UUID multiplierId,
        BigDecimal multiplierRate,
        String multiplierName,
        int count,
        BigDecimal percentage
    ) {}

    private record Bucket(int count, BigDecimal rate) {
        Bucket inc(BigDecimal rate) { return new Bucket(count + 1, rate); }
    }

    @Transactional(readOnly = true)
    public List<MultiplierUsagePoint> handle(Query query) {
        // Resolver id → nombre. Multiplicadores borrados quedan con fallback "× rate".
        Map<UUID, String> names = new HashMap<>();
        for (MultiplierSnapshot ms : tripsApi.listMultipliersByUser(query.userId())) {
            names.put(ms.id(), ms.name());
        }

        Map<UUID, Bucket> counts = new HashMap<>();
        for (TripSnapshot t : tripsApi.listCompletedByUser(query.userId(), null, null)) {
            if (t.multiplierId() == null || t.multiplierRate() == null) continue;
            UUID mid = t.multiplierId();
            counts.merge(mid, new Bucket(1, t.multiplierRate()),
                (existing, fresh) -> existing.inc(fresh.rate()));
        }

        int total = counts.values().stream().mapToInt(Bucket::count).sum();
        List<MultiplierUsagePoint> points = new ArrayList<>();
        for (var entry : counts.entrySet()) {
            UUID mid = entry.getKey();
            Bucket b = entry.getValue();
            BigDecimal percentage = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(b.count())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
            String name = names.getOrDefault(
                mid,
                "× " + b.rate().stripTrailingZeros().toPlainString()
            );
            points.add(new MultiplierUsagePoint(mid, b.rate(), name, b.count(), percentage));
        }
        // Más usado primero.
        points.sort(Comparator.comparingInt(MultiplierUsagePoint::count).reversed());
        return points;
    }
}
