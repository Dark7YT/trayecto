package com.trayecto.trips.interfaces.rest.dto;

import com.trayecto.shared.kernel.Money;
import com.trayecto.trips.api.dto.TripSnapshot;
import com.trayecto.trips.domain.Trip;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TripResponse(
    UUID id,
    String name,
    BigDecimal startKm,
    BigDecimal endKm,
    BigDecimal distanceKm,
    UUID multiplierId,
    BigDecimal multiplierRate,
    BigDecimal totalCostAmount,
    String totalCostCurrency,
    String status,
    String startPhotoUrl,
    String endPhotoUrl,
    Instant startedAt,
    Instant completedAt,
    Instant cancelledAt
) {
    public static TripResponse from(Trip trip) {
        BigDecimal distance = trip.endKm()
            .map(end -> trip.startKm().distanceTo(end).value())
            .orElse(null);
        return new TripResponse(
            trip.id().value(),
            trip.name().value(),
            trip.startKm().value(),
            trip.endKm().map(k -> k.value()).orElse(null),
            distance,
            trip.multiplierId().map(m -> m.value()).orElse(null),
            trip.multiplierRate().orElse(null),
            trip.totalCost().map(Money::amount).orElse(null),
            trip.totalCost().map(m -> m.currency().getCurrencyCode()).orElse(null),
            trip.status().name(),
            trip.startPhotoUrl().orElse(null),
            trip.endPhotoUrl().orElse(null),
            trip.startedAt(),
            trip.completedAt().orElse(null),
            trip.cancelledAt().orElse(null)
        );
    }

    public static TripResponse from(TripSnapshot snapshot) {
        return new TripResponse(
            snapshot.tripId().value(),
            snapshot.name(),
            snapshot.startKm(),
            snapshot.endKm(),
            snapshot.distanceKm(),
            snapshot.multiplierId(),
            snapshot.multiplierRate(),
            snapshot.totalCost() != null ? snapshot.totalCost().amount() : null,
            snapshot.totalCost() != null ? snapshot.totalCost().currency().getCurrencyCode() : null,
            snapshot.status(),
            snapshot.startPhotoUrl(),
            snapshot.endPhotoUrl(),
            snapshot.startedAt(),
            snapshot.completedAt(),
            snapshot.cancelledAt()
        );
    }
}
