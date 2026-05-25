package com.trayecto.sharing.api.events;

import com.trayecto.shared.kernel.UserId;
import java.util.UUID;

import java.time.Instant;

public record AccessGrantRejected(
    UUID grantId,
    UserId ownerId,
    UserId granteeId,
    Instant rejectedAt
) {}
