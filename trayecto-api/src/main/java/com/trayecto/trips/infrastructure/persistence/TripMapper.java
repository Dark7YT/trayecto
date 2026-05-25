package com.trayecto.trips.infrastructure.persistence;

import com.trayecto.shared.kernel.Kilometers;
import com.trayecto.shared.kernel.Money;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.domain.MultiplierId;
import com.trayecto.trips.domain.Trip;
import com.trayecto.shared.kernel.TripId;
import com.trayecto.trips.domain.TripName;

import java.math.BigDecimal;
import java.util.Currency;

final class TripMapper {

    private TripMapper() {}

    static TripEntity toEntity(Trip trip, TripEntity reuse) {
        TripEntity e = reuse != null ? reuse : new TripEntity();
        e.id = trip.id().value();
        e.userId = trip.userId().value();
        e.name = trip.name().value();
        e.startKm = trip.startKm().value();
        e.endKm = trip.endKm().map(Kilometers::value).orElse(null);
        e.multiplierId = trip.multiplierId().map(MultiplierId::value).orElse(null);
        e.multiplierRate = trip.multiplierRate().orElse(null);
        e.totalCostAmount = trip.totalCost().map(Money::amount).orElse(null);
        e.totalCostCurrency = trip.totalCost().map(m -> m.currency().getCurrencyCode()).orElse(null);
        e.status = trip.status();
        e.startPhotoUrl = trip.startPhotoUrl().orElse(null);
        e.endPhotoUrl = trip.endPhotoUrl().orElse(null);
        e.startedAt = trip.startedAt();
        e.completedAt = trip.completedAt().orElse(null);
        e.cancelledAt = trip.cancelledAt().orElse(null);
        e.updatedAt = trip.updatedAt();
        e.deletedAt = trip.deletedAt().orElse(null);
        return e;
    }

    static Trip toDomain(TripEntity e) {
        Money totalCost = null;
        if (e.totalCostAmount != null && e.totalCostCurrency != null) {
            totalCost = new Money(e.totalCostAmount, Currency.getInstance(e.totalCostCurrency));
        }
        BigDecimal endKmValue = e.endKm;
        return Trip.reconstitute(
            TripId.of(e.id),
            UserId.of(e.userId),
            new TripName(e.name),
            Kilometers.of(e.startKm),
            endKmValue != null ? Kilometers.of(endKmValue) : null,
            e.multiplierId != null ? MultiplierId.of(e.multiplierId) : null,
            e.multiplierRate,
            totalCost,
            e.status,
            e.startPhotoUrl,
            e.endPhotoUrl,
            e.startedAt,
            e.completedAt,
            e.cancelledAt,
            e.updatedAt,
            e.deletedAt
        );
    }
}
