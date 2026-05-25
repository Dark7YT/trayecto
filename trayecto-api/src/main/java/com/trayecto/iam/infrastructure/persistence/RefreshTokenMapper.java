package com.trayecto.iam.infrastructure.persistence;

import com.trayecto.iam.domain.RefreshToken;
import com.trayecto.iam.domain.TokenId;
import com.trayecto.shared.kernel.UserId;

final class RefreshTokenMapper {

    private RefreshTokenMapper() {}

    static RefreshTokenEntity toEntity(RefreshToken token, RefreshTokenEntity reuse) {
        RefreshTokenEntity entity = reuse != null ? reuse : new RefreshTokenEntity();
        entity.id = token.id().value();
        entity.userId = token.userId().value();
        entity.hashedToken = token.hashedToken();
        entity.familyId = token.familyId();
        entity.expiresAt = token.expiresAt();
        entity.createdAt = token.createdAt();
        entity.deviceFingerprint = token.deviceFingerprint().orElse(null);
        entity.revokedAt = token.revokedAt().orElse(null);
        entity.replacedByTokenId = token.replacedByTokenId().map(TokenId::value).orElse(null);
        return entity;
    }

    static RefreshToken toDomain(RefreshTokenEntity entity) {
        return RefreshToken.reconstitute(
            TokenId.of(entity.id),
            UserId.of(entity.userId),
            entity.hashedToken,
            entity.familyId,
            entity.expiresAt,
            entity.createdAt,
            entity.deviceFingerprint,
            entity.revokedAt,
            entity.replacedByTokenId != null ? TokenId.of(entity.replacedByTokenId) : null
        );
    }
}
