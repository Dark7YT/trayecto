package com.trayecto.iam.infrastructure.persistence;

import com.trayecto.iam.domain.RefreshToken;
import com.trayecto.iam.domain.RefreshTokenRepository;
import com.trayecto.shared.kernel.UserId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<RefreshToken> findByHashedToken(String hashedToken) {
        return jpa.findByHashedToken(hashedToken).map(RefreshTokenMapper::toDomain);
    }

    @Override
    public List<RefreshToken> findActiveByUserId(UserId userId) {
        return jpa.findByUserIdAndRevokedAtIsNull(userId.value())
            .stream().map(RefreshTokenMapper::toDomain).toList();
    }

    @Override
    public List<RefreshToken> findByFamilyId(UUID familyId) {
        return jpa.findByFamilyId(familyId).stream().map(RefreshTokenMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public int revokeFamily(UUID familyId, Instant revokedAt) {
        return jpa.revokeFamily(familyId, revokedAt);
    }

    @Override
    @Transactional
    public int revokeAllForUser(UserId userId, Instant revokedAt) {
        return jpa.revokeAllForUser(userId.value(), revokedAt);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenEntity existing = jpa.findById(token.id().value()).orElse(null);
        RefreshTokenEntity entity = RefreshTokenMapper.toEntity(token, existing);
        return RefreshTokenMapper.toDomain(jpa.save(entity));
    }

    @Override
    @Transactional
    public int deleteExpiredBefore(Instant cutoff) {
        return jpa.deleteExpiredBefore(cutoff);
    }
}
