package com.trayecto.iam.infrastructure.persistence;

import com.trayecto.iam.domain.DisplayName;
import com.trayecto.iam.domain.PasswordHash;
import com.trayecto.iam.domain.User;
import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

/**
 * Conversor entity ↔ domain. No usa MapStruct porque los mapeos involucran VOs
 * con constructores validados que rechazan nulls — más explícito mantenerlo a mano
 * y evita comportamientos inesperados del generador.
 */
final class UserMapper {

    private UserMapper() {}

    static UserEntity toEntity(User user, UserEntity reuse) {
        UserEntity entity = reuse != null ? reuse : new UserEntity();
        entity.id = user.id().value();
        entity.email = user.email().value();
        entity.passwordHash = user.passwordHash().map(PasswordHash::value).orElse(null);
        entity.displayName = user.displayName().value();
        entity.locale = user.locale().toLanguageTag();
        entity.timezone = user.timezone().getId();
        entity.status = user.status();
        entity.provider = user.provider();
        entity.googleSubject = user.googleSubject().orElse(null);
        entity.createdAt = user.createdAt();
        entity.updatedAt = user.updatedAt();
        return entity;
    }

    static User toDomain(UserEntity entity) {
        return User.reconstitute(
            UserId.of(entity.id),
            Email.of(entity.email),
            Optional.ofNullable(entity.passwordHash).map(PasswordHash::new).orElse(null),
            new DisplayName(entity.displayName),
            Locale.forLanguageTag(entity.locale),
            ZoneId.of(entity.timezone),
            entity.status,
            entity.provider,
            entity.googleSubject,
            entity.createdAt,
            entity.updatedAt
        );
    }
}
