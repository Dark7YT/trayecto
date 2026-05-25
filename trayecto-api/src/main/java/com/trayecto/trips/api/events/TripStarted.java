package com.trayecto.trips.api.events;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.TripId;

import java.math.BigDecimal;
import java.time.Instant;

public record TripStarted(
    TripId tripId,
    UserId userId,
    String name,
    BigDecimal startKm,
    Instant startedAt
) {}
