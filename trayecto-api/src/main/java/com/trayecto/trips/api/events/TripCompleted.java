package com.trayecto.trips.api.events;

import com.trayecto.shared.kernel.Money;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.TripId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Emitido cuando un viaje se cierra. {@code analytics} actualiza read-models;
 * {@code budget listener} suma el costo al presupuesto mensual del usuario;
 * {@code notifications} envía push WebSocket al owner.
 * <p>
 * {@code multiplierId} se expone como {@link UUID} para no acoplar consumidores
 * externos al VO interno de trips.
 */
public record TripCompleted(
    TripId tripId,
    UserId userId,
    UUID multiplierId,
    BigDecimal distanceKm,
    Money totalCost,
    BigDecimal multiplierRate,
    Instant completedAt
) {}
