package com.trayecto.trips.api.events;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.TripId;

import java.time.Instant;

public record TripCancelled(
    TripId tripId,
    UserId userId,
    Instant cancelledAt
) {}
