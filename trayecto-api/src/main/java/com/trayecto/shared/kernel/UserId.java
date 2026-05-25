package com.trayecto.shared.kernel;

import java.util.Objects;
import java.util.UUID;

/**
 * Identidad de usuario. Compartido como kernel para que cualquier módulo pueda
 * referenciar usuarios por valor sin acoplarse al agregado {@code User} (que vive en iam).
 */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "UserId.value must not be null");
    }

    public static UserId newId() {
        return new UserId(UuidV7.randomUuid());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId of(String value) {
        return new UserId(UUID.fromString(value));
    }

    public String asString() {
        return value.toString();
    }
}
