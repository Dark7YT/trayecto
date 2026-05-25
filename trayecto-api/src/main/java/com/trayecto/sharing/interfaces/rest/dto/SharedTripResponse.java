package com.trayecto.sharing.interfaces.rest.dto;

import com.trayecto.shared.kernel.Money;
import com.trayecto.trips.api.dto.TripSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO HTTP plano para representar un viaje compartido. Aplana los VOs internos
 * ({@code TripId}, {@code UserId}) a UUID para que el cliente consuma strings,
 * no objetos {@code { value: "..." }}.
 */
public record SharedTripResponse(
    UUID tripId,
    UUID userId,
    String name,
    BigDecimal startKm,
    BigDecimal endKm,
    BigDecimal distanceKm,
    BigDecimal multiplierRate,
    Money totalCost,
    String status,
    String startPhotoUrl,
    String endPhotoUrl,
    Instant startedAt,
    Instant completedAt,
    Instant cancelledAt
) {
    public static SharedTripResponse from(TripSnapshot s) {
        return new SharedTripResponse(
            s.tripId().value(),
            s.userId().value(),
            s.name(),
            s.startKm(),
            s.endKm(),
            s.distanceKm(),
            s.multiplierRate(),
            s.totalCost(),
            s.status(),
            s.startPhotoUrl(),
            s.endPhotoUrl(),
            s.startedAt(),
            s.completedAt(),
            s.cancelledAt()
        );
    }
}
