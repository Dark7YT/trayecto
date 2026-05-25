package com.trayecto.iam.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "iam_refresh_tokens",
    indexes = {
        @Index(name = "ix_iam_refresh_tokens_hashed_token", columnList = "hashed_token", unique = true),
        @Index(name = "ix_iam_refresh_tokens_user_id", columnList = "user_id"),
        @Index(name = "ix_iam_refresh_tokens_family_id", columnList = "family_id"),
        @Index(name = "ix_iam_refresh_tokens_expires_at", columnList = "expires_at")
    }
)
class RefreshTokenEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "hashed_token", nullable = false, length = 64, unique = true)
    String hashedToken;

    @Column(name = "family_id", nullable = false)
    UUID familyId;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "device_fingerprint", length = 128)
    String deviceFingerprint;

    @Column(name = "revoked_at")
    Instant revokedAt;

    @Column(name = "replaced_by_token_id")
    UUID replacedByTokenId;

    RefreshTokenEntity() {}
}
