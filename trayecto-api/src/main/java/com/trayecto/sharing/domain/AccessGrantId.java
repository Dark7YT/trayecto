package com.trayecto.sharing.domain;

import com.trayecto.shared.kernel.UuidV7;

import java.util.Objects;
import java.util.UUID;

public record AccessGrantId(UUID value) {

    public AccessGrantId {
        Objects.requireNonNull(value, "AccessGrantId.value must not be null");
    }

    public static AccessGrantId newId() {
        return new AccessGrantId(UuidV7.randomUuid());
    }

    public static AccessGrantId of(UUID value) {
        return new AccessGrantId(value);
    }

    public static AccessGrantId of(String value) {
        return new AccessGrantId(UUID.fromString(value));
    }
}
