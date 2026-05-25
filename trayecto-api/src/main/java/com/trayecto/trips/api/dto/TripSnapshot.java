package com.trayecto.trips.api.dto;

import com.trayecto.shared.kernel.Money;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.TripId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Vista pública de un viaje. {@code sharing} y {@code analytics} consumen este snapshot
 * sin acceder al agregado interno. {@code multiplierId} se expone como {@link UUID}
 * para que analytics pueda reconstruir read-models desde viajes existentes.
 */
public record TripSnapshot(
    TripId tripId,
    UserId userId,
    UUID multiplierId,           // null si PENDING
    String name,
    BigDecimal startKm,
    BigDecimal endKm,            // null si PENDING
    BigDecimal distanceKm,       // null si PENDING
    BigDecimal multiplierRate,   // null si PENDING
    Money totalCost,             // null si PENDING/CANCELLED
    String status,
    String startPhotoUrl,
    String endPhotoUrl,
    Instant startedAt,
    Instant completedAt,
    Instant cancelledAt
) {}
