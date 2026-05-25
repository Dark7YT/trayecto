package com.trayecto.iam.infrastructure.persistence;

import com.trayecto.iam.domain.EmailVerificationToken;
import com.trayecto.iam.domain.TokenId;
import com.trayecto.shared.kernel.UserId;

final class EmailVerificationTokenMapper {

    private EmailVerificationTokenMapper() {}

    static EmailVerificationTokenEntity toEntity(EmailVerificationToken token,
                                                  EmailVerificationTokenEntity reuse) {
        EmailVerificationTokenEntity entity = reuse != null ? reuse : new EmailVerificationTokenEntity();
        entity.id = token.id().value();
        entity.userId = token.userId().value();
        entity.hashedToken = token.hashedToken();
        entity.expiresAt = token.expiresAt();
        entity.createdAt = token.createdAt();
        entity.consumedAt = token.consumedAt().orElse(null);
        return entity;
    }

    static EmailVerificationToken toDomain(EmailVerificationTokenEntity entity) {
        return EmailVerificationToken.reconstitute(
            TokenId.of(entity.id),
            UserId.of(entity.userId),
            entity.hashedToken,
            entity.expiresAt,
            entity.createdAt,
            entity.consumedAt
        );
    }
}
