package com.trayecto.sharing.infrastructure.persistence;

import com.trayecto.sharing.domain.AccessGrantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "sharing_access_grants",
    indexes = {
        @Index(name = "ix_sharing_grant_owner", columnList = "owner_id"),
        @Index(name = "ix_sharing_grant_grantee_id", columnList = "grantee_id"),
        @Index(name = "ix_sharing_grant_grantee_email", columnList = "grantee_email"),
        @Index(name = "ix_sharing_grant_token", columnList = "invite_token_hash", unique = true)
    }
)
class AccessGrantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "owner_id", nullable = false)
    UUID ownerId;

    @Column(name = "grantee_email", nullable = false, length = 254)
    String granteeEmail;

    @Column(name = "grantee_id")
    UUID granteeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    AccessGrantStatus status;

    @Column(name = "invite_token_hash", nullable = false, length = 64, unique = true)
    String inviteTokenHash;

    @Column(name = "invited_at", nullable = false)
    Instant invitedAt;

    @Column(name = "responded_at")
    Instant respondedAt;

    @Column(name = "revoked_at")
    Instant revokedAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    AccessGrantEntity() {}
}
