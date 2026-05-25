package com.trayecto.trips.api.events;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.TripId;

import java.time.Instant;

public record TripEdited(
    TripId tripId,
    UserId userId,
    Instant editedAt
) {}
