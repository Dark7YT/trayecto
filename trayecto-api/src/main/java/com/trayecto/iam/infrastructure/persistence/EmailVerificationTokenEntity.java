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
    name = "iam_email_verification_tokens",
    indexes = {
        @Index(name = "ix_iam_evt_hashed", columnList = "hashed_token", unique = true),
        @Index(name = "ix_iam_evt_user_id", columnList = "user_id"),
        @Index(name = "ix_iam_evt_expires_at", columnList = "expires_at")
    }
)
class EmailVerificationTokenEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "hashed_token", nullable = false, length = 64, unique = true)
    String hashedToken;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "consumed_at")
    Instant consumedAt;

    EmailVerificationTokenEntity() {}
}
