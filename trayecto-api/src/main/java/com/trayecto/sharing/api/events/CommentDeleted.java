package com.trayecto.sharing.api.events;

import com.trayecto.shared.kernel.UserId;
import java.util.UUID;
import com.trayecto.shared.kernel.TripId;

import java.time.Instant;

public record CommentDeleted(
    UUID commentId,
    TripId tripId,
    UserId authorId,
    Instant deletedAt
) {}
