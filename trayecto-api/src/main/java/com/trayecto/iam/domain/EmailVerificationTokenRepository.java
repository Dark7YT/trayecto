package com.trayecto.iam.domain;

import com.trayecto.shared.kernel.UserId;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationTokenRepository {

    Optional<EmailVerificationToken> findByHashedToken(String hashedToken);

    /** Invalida cualquier token previo del usuario (al reenviar email de verificación). */
    int invalidatePreviousFor(UserId userId, Instant consumedAt);

    EmailVerificationToken save(EmailVerificationToken token);

    int deleteExpiredBefore(Instant cutoff);
}
