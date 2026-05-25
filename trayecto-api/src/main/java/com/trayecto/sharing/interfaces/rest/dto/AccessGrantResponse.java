package com.trayecto.sharing.interfaces.rest.dto;

import com.trayecto.sharing.domain.AccessGrant;

import java.time.Instant;
import java.util.UUID;

public record AccessGrantResponse(
    UUID id,
    UUID ownerId,
    String granteeEmail,
    UUID granteeId,
    String status,
    Instant invitedAt,
    Instant respondedAt,
    Instant revokedAt
) {
    public static AccessGrantResponse from(AccessGrant grant) {
        return new AccessGrantResponse(
            grant.id().value(),
            grant.ownerId().value(),
            grant.granteeEmail().value(),
            grant.granteeId().map(g -> g.value()).orElse(null),
            grant.status().name(),
            grant.invitedAt(),
            grant.respondedAt().orElse(null),
            grant.revokedAt().orElse(null)
        );
    }
}
