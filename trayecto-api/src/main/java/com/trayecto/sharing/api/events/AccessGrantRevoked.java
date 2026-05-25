package com.trayecto.sharing.api.events;

import com.trayecto.shared.kernel.UserId;
import java.util.UUID;

import java.time.Instant;

public record AccessGrantRevoked(
    UUID grantId,
    UserId ownerId,
    UserId granteeId,  // null si la invitación nunca fue aceptada
    Instant revokedAt
) {}
