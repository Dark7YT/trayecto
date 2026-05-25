package com.trayecto.trips.domain;

import com.trayecto.shared.kernel.UuidV7;

import java.util.Objects;
import java.util.UUID;

public record MultiplierId(UUID value) {

    public MultiplierId {
        Objects.requireNonNull(value, "MultiplierId.value must not be null");
    }

    public static MultiplierId newId() {
        return new MultiplierId(UuidV7.randomUuid());
    }

    public static MultiplierId of(UUID value) {
        return new MultiplierId(value);
    }

    public static MultiplierId of(String value) {
        return new MultiplierId(UUID.fromString(value));
    }
}
