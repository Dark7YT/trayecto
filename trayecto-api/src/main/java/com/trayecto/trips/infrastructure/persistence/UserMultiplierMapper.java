package com.trayecto.trips.infrastructure.persistence;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.domain.MultiplierId;
import com.trayecto.trips.domain.UserMultiplier;

final class UserMultiplierMapper {

    private UserMultiplierMapper() {}

    static UserMultiplierEntity toEntity(UserMultiplier m, UserMultiplierEntity reuse) {
        UserMultiplierEntity e = reuse != null ? reuse : new UserMultiplierEntity();
        e.id = m.id().value();
        e.userId = m.userId().value();
        e.name = m.name();
        e.value = m.value();
        e.isDefault = m.isDefault();
        e.createdAt = m.createdAt();
        e.updatedAt = m.updatedAt();
        return e;
    }

    static UserMultiplier toDomain(UserMultiplierEntity e) {
        return UserMultiplier.reconstitute(
            MultiplierId.of(e.id),
            UserId.of(e.userId),
            e.name,
            e.value,
            e.isDefault,
            e.createdAt,
            e.updatedAt
        );
    }
}
