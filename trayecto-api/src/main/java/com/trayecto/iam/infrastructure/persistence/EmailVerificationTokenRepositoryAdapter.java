package com.trayecto.iam.infrastructure.persistence;

import com.trayecto.iam.domain.EmailVerificationToken;
import com.trayecto.iam.domain.EmailVerificationTokenRepository;
import com.trayecto.shared.kernel.UserId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
class EmailVerificationTokenRepositoryAdapter implements EmailVerificationTokenRepository {

    private final EmailVerificationTokenJpaRepository jpa;

    EmailVerificationTokenRepositoryAdapter(EmailVerificationTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<EmailVerificationToken> findByHashedToken(String hashedToken) {
        return jpa.findByHashedToken(hashedToken).map(EmailVerificationTokenMapper::toDomain);
    }

    @Override
    @Transactional
    public int invalidatePreviousFor(UserId userId, Instant consumedAt) {
        return jpa.invalidatePreviousFor(userId.value(), consumedAt);
    }

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        EmailVerificationTokenEntity existing = jpa.findById(token.id().value()).orElse(null);
        EmailVerificationTokenEntity entity = EmailVerificationTokenMapper.toEntity(token, existing);
        return EmailVerificationTokenMapper.toDomain(jpa.save(entity));
    }

    @Override
    @Transactional
    public int deleteExpiredBefore(Instant cutoff) {
        return jpa.deleteExpiredBefore(cutoff);
    }
}
