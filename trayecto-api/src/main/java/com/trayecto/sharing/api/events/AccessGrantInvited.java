package com.trayecto.sharing.api.events;

import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;
import java.util.UUID;

import java.time.Instant;

/**
 * El owner invitó a alguien (por email) a ver sus viajes. {@code notifications} envía
 * un email al {@code granteeEmail} con un link `/shared/accept?token=...` para responder.
 * El token raw solo existe en este momento (solo el hash queda en BD).
 */
public record AccessGrantInvited(
    UUID grantId,
    UserId ownerId,
    Email granteeEmail,
    String inviteTokenRaw,
    Instant invitedAt
) {}
