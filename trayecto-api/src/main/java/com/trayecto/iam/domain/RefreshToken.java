package com.trayecto.iam.domain;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh token opaco (UUID v7) persistido como hash SHA-256.
 * <p>
 * Modelo de rotación:
 * - Cada token tiene un {@code familyId}: comparten id todos los tokens de una sesión.
 * - {@code refresh} crea un nuevo token y revoca el actual ({@code replacedByTokenId} apunta al nuevo).
 * - Si llega un token revocado ({@link #isRevoked()}), se considera reuso (robo o bug) y
 *   el handler debe revocar toda la familia + emitir {@code RefreshTokenReuseDetected}.
 * <p>
 * El raw token NUNCA se persiste, solo su hash. El raw solo vive en cookies del cliente.
 */
public final class RefreshToken {

    private final TokenId id;
    private final UserId userId;
    private final String hashedToken;
    private final UUID familyId;
    private final Instant expiresAt;
    private final Instant createdAt;
    private final String deviceFingerprint; // nullable
    private Instant revokedAt; // null si vigente
    private TokenId replacedByTokenId; // null si no rotado

    private RefreshToken(
        TokenId id,
        UserId userId,
        String hashedToken,
        UUID familyId,
        Instant expiresAt,
        Instant createdAt,
        String deviceFingerprint,
        Instant revokedAt,
        TokenId replacedByTokenId
    ) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.hashedToken = Objects.requireNonNull(hashedToken);
        this.familyId = Objects.requireNonNull(familyId);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.deviceFingerprint = deviceFingerprint;
        this.revokedAt = revokedAt;
        this.replacedByTokenId = replacedByTokenId;
    }

    public static RefreshToken issue(
        UserId userId,
        String hashedToken,
        UUID familyId,
        Instant expiresAt,
        String deviceFingerprint
    ) {
        Instant now = Instant.now();
        if (!expiresAt.isAfter(now)) {
            throw new BusinessRuleViolation("refresh_token.bad_expiry",
                "expiresAt must be in the future");
        }
        return new RefreshToken(
            TokenId.newId(), userId, hashedToken, familyId,
            expiresAt, now, deviceFingerprint, null, null
        );
    }

    /** Nueva familia (login fresh, no rotación). */
    public static RefreshToken issueNewFamily(
        UserId userId,
        String hashedToken,
        Instant expiresAt,
        String deviceFingerprint
    ) {
        return issue(userId, hashedToken, UUID.randomUUID(), expiresAt, deviceFingerprint);
    }

    public static RefreshToken reconstitute(
        TokenId id, UserId userId, String hashedToken, UUID familyId,
        Instant expiresAt, Instant createdAt, String deviceFingerprint,
        Instant revokedAt, TokenId replacedByTokenId
    ) {
        return new RefreshToken(id, userId, hashedToken, familyId,
            expiresAt, createdAt, deviceFingerprint, revokedAt, replacedByTokenId);
    }

    /**
     * Rota este token. Marca el actual como revocado apuntando al nuevo y retorna el nuevo.
     * El nuevo conserva el mismo {@code familyId}.
     */
    public RefreshToken rotateTo(String newHashedToken, Instant newExpiresAt, String deviceFingerprint) {
        if (isRevoked()) {
            throw new BusinessRuleViolation("refresh_token.already_revoked",
                "Cannot rotate an already revoked token");
        }
        if (isExpired()) {
            throw new BusinessRuleViolation("refresh_token.expired",
                "Cannot rotate an expired token");
        }
        RefreshToken newToken = issue(userId, newHashedToken, familyId, newExpiresAt, deviceFingerprint);
        this.revokedAt = Instant.now();
        this.replacedByTokenId = newToken.id;
        return newToken;
    }

    public void revoke() {
        if (isRevoked()) return;
        this.revokedAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isUsable() {
        return !isRevoked() && !isExpired();
    }

    public TokenId id() { return id; }
    public UserId userId() { return userId; }
    public String hashedToken() { return hashedToken; }
    public UUID familyId() { return familyId; }
    public Instant expiresAt() { return expiresAt; }
    public Instant createdAt() { return createdAt; }
    public Optional<String> deviceFingerprint() { return Optional.ofNullable(deviceFingerprint); }
    public Optional<Instant> revokedAt() { return Optional.ofNullable(revokedAt); }
    public Optional<TokenId> replacedByTokenId() { return Optional.ofNullable(replacedByTokenId); }
}
