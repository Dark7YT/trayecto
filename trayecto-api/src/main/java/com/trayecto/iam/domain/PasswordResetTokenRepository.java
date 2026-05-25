package com.trayecto.iam.domain;

import com.trayecto.shared.kernel.UserId;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository {

    Optional<PasswordResetToken> findByHashedToken(String hashedToken);

    /** Invalida cualquier token previo del usuario (al solicitar otro reset). */
    int invalidatePreviousFor(UserId userId, Instant consumedAt);

    PasswordResetToken save(PasswordResetToken token);

    int deleteExpiredBefore(Instant cutoff);
}
