package com.trayecto.sharing.api.dto;

import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.sharing.domain.AccessGrantId;
import com.trayecto.sharing.domain.AccessGrantStatus;

import java.time.Instant;

public record AccessGrantSnapshot(
    AccessGrantId id,
    UserId ownerId,
    Email granteeEmail,
    UserId granteeId,        // null si todavía no aceptó
    AccessGrantStatus status,
    Instant invitedAt,
    Instant respondedAt,
    Instant revokedAt
) {}
