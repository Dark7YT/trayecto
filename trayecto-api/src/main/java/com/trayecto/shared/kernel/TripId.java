package com.trayecto.shared.kernel;

import java.util.Objects;
import java.util.UUID;

/**
 * Identidad de viaje. Vive en {@code shared.kernel} porque módulos como {@code sharing}
 * (que comenta sobre viajes) y {@code analytics} (que mantiene read-models por viaje)
 * la referencian por valor, sin acceder al agregado interno de {@code trips}.
 */
public record TripId(UUID value) {

    public TripId {
        Objects.requireNonNull(value, "TripId.value must not be null");
    }

    public static TripId newId() {
        return new TripId(UuidV7.randomUuid());
    }

    public static TripId of(UUID value) {
        return new TripId(value);
    }

    public static TripId of(String value) {
        return new TripId(UUID.fromString(value));
    }

    public String asString() {
        return value.toString();
    }
}
