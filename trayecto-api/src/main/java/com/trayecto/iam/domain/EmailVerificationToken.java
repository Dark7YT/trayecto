package com.trayecto.iam.domain;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Token de un solo uso para verificar el email de un usuario al registrarse.
 * El raw token (UUID v7) se envía por correo; aquí solo se persiste el hash SHA-256.
 * TTL típico: 24 horas.
 */
public final class EmailVerificationToken {

    private final TokenId id;
    private final UserId userId;
    private final String hashedToken;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant consumedAt; // null mientras esté pendiente

    private EmailVerificationToken(
        TokenId id, UserId userId, String hashedToken,
        Instant expiresAt, Instant createdAt, Instant consumedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.hashedToken = Objects.requireNonNull(hashedToken);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.consumedAt = consumedAt;
    }

    public static EmailVerificationToken issue(UserId userId, String hashedToken, Instant expiresAt) {
        Instant now = Instant.now();
        if (!expiresAt.isAfter(now)) {
            throw new BusinessRuleViolation("email_verification_token.bad_expiry",
                "expiresAt must be in the future");
        }
        return new EmailVerificationToken(TokenId.newId(), userId, hashedToken, expiresAt, now, null);
    }

    public static EmailVerificationToken reconstitute(
        TokenId id, UserId userId, String hashedToken,
        Instant expiresAt, Instant createdAt, Instant consumedAt
    ) {
        return new EmailVerificationToken(id, userId, hashedToken, expiresAt, createdAt, consumedAt);
    }

    public void consume() {
        if (isConsumed()) {
            throw new BusinessRuleViolation("email_verification_token.already_consumed",
                "This verification token has already been used");
        }
        if (isExpired()) {
            throw new BusinessRuleViolation("email_verification_token.expired",
                "This verification token has expired");
        }
        this.consumedAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public TokenId id() { return id; }
    public UserId userId() { return userId; }
    public String hashedToken() { return hashedToken; }
    public Instant expiresAt() { return expiresAt; }
    public Instant createdAt() { return createdAt; }
    public Optional<Instant> consumedAt() { return Optional.ofNullable(consumedAt); }
}
