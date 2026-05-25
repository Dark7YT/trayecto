package com.trayecto.iam.domain;

import com.trayecto.shared.kernel.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByHashedToken(String hashedToken);

    List<RefreshToken> findActiveByUserId(UserId userId);

    List<RefreshToken> findByFamilyId(UUID familyId);

    /** Marca como revocada toda la familia. Útil al detectar reuso. */
    int revokeFamily(UUID familyId, Instant revokedAt);

    /** Revoca todos los tokens activos del usuario. Útil en logout-all o cambio de password. */
    int revokeAllForUser(UserId userId, Instant revokedAt);

    RefreshToken save(RefreshToken token);

    /** Borra tokens expirados antes del cutoff. Llamado por job programado. */
    int deleteExpiredBefore(Instant cutoff);
}
