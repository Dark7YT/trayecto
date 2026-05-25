package com.trayecto.iam.infrastructure.persistence;

import com.trayecto.iam.domain.PasswordResetToken;
import com.trayecto.iam.domain.PasswordResetTokenRepository;
import com.trayecto.shared.kernel.UserId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpa;

    PasswordResetTokenRepositoryAdapter(PasswordResetTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<PasswordResetToken> findByHashedToken(String hashedToken) {
        return jpa.findByHashedToken(hashedToken).map(PasswordResetTokenMapper::toDomain);
    }

    @Override
    @Transactional
    public int invalidatePreviousFor(UserId userId, Instant consumedAt) {
        return jpa.invalidatePreviousFor(userId.value(), consumedAt);
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenEntity existing = jpa.findById(token.id().value()).orElse(null);
        PasswordResetTokenEntity entity = PasswordResetTokenMapper.toEntity(token, existing);
        return PasswordResetTokenMapper.toDomain(jpa.save(entity));
    }

    @Override
    @Transactional
    public int deleteExpiredBefore(Instant cutoff) {
        return jpa.deleteExpiredBefore(cutoff);
    }
}
