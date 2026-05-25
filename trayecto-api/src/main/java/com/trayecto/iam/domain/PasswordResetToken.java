package com.trayecto.iam.domain;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Token de un solo uso para resetear el password. TTL corto: 1 hora.
 * Se invalida cualquier otro token previo del mismo usuario al emitir uno nuevo.
 */
public final class PasswordResetToken {

    private final TokenId id;
    private final UserId userId;
    private final String hashedToken;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant consumedAt;

    private PasswordResetToken(
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

    public static PasswordResetToken issue(UserId userId, String hashedToken, Instant expiresAt) {
        Instant now = Instant.now();
        if (!expiresAt.isAfter(now)) {
            throw new BusinessRuleViolation("password_reset_token.bad_expiry",
                "expiresAt must be in the future");
        }
        return new PasswordResetToken(TokenId.newId(), userId, hashedToken, expiresAt, now, null);
    }

    public static PasswordResetToken reconstitute(
        TokenId id, UserId userId, String hashedToken,
        Instant expiresAt, Instant createdAt, Instant consumedAt
    ) {
        return new PasswordResetToken(id, userId, hashedToken, expiresAt, createdAt, consumedAt);
    }

    public void consume() {
        if (isConsumed()) {
            throw new BusinessRuleViolation("password_reset_token.already_consumed",
                "This password reset token has already been used");
        }
        if (isExpired()) {
            throw new BusinessRuleViolation("password_reset_token.expired",
                "This password reset token has expired");
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
