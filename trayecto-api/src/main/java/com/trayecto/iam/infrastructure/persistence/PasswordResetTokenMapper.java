package com.trayecto.iam.infrastructure.persistence;

import com.trayecto.iam.domain.PasswordResetToken;
import com.trayecto.iam.domain.TokenId;
import com.trayecto.shared.kernel.UserId;

final class PasswordResetTokenMapper {

    private PasswordResetTokenMapper() {}

    static PasswordResetTokenEntity toEntity(PasswordResetToken token, PasswordResetTokenEntity reuse) {
        PasswordResetTokenEntity entity = reuse != null ? reuse : new PasswordResetTokenEntity();
        entity.id = token.id().value();
        entity.userId = token.userId().value();
        entity.hashedToken = token.hashedToken();
        entity.expiresAt = token.expiresAt();
        entity.createdAt = token.createdAt();
        entity.consumedAt = token.consumedAt().orElse(null);
        return entity;
    }

    static PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        return PasswordResetToken.reconstitute(
            TokenId.of(entity.id),
            UserId.of(entity.userId),
            entity.hashedToken,
            entity.expiresAt,
            entity.createdAt,
            entity.consumedAt
        );
    }
}
