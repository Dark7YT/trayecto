package com.trayecto.sharing.infrastructure.persistence;

import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.sharing.domain.AccessGrant;
import com.trayecto.sharing.domain.AccessGrantId;

final class AccessGrantMapper {

    private AccessGrantMapper() {}

    static AccessGrantEntity toEntity(AccessGrant grant, AccessGrantEntity reuse) {
        AccessGrantEntity e = reuse != null ? reuse : new AccessGrantEntity();
        e.id = grant.id().value();
        e.ownerId = grant.ownerId().value();
        e.granteeEmail = grant.granteeEmail().value();
        e.granteeId = grant.granteeId().map(UserId::value).orElse(null);
        e.status = grant.status();
        e.inviteTokenHash = grant.inviteTokenHash();
        e.invitedAt = grant.invitedAt();
        e.respondedAt = grant.respondedAt().orElse(null);
        e.revokedAt = grant.revokedAt().orElse(null);
        e.updatedAt = grant.updatedAt();
        return e;
    }

    static AccessGrant toDomain(AccessGrantEntity e) {
        return AccessGrant.reconstitute(
            AccessGrantId.of(e.id),
            UserId.of(e.ownerId),
            Email.of(e.granteeEmail),
            e.granteeId != null ? UserId.of(e.granteeId) : null,
            e.status,
            e.inviteTokenHash,
            e.invitedAt,
            e.respondedAt,
            e.revokedAt,
            e.updatedAt
        );
    }
}
