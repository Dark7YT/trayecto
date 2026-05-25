package com.trayecto.iam.domain;

import com.trayecto.shared.kernel.UuidV7;

import java.util.Objects;
import java.util.UUID;

/**
 * Identidad de un token (refresh, verification, reset). UUID v7 para que el orden
 * temporal de creación se preserve en índices BTree.
 */
public record TokenId(UUID value) {

    public TokenId {
        Objects.requireNonNull(value, "TokenId.value must not be null");
    }

    public static TokenId newId() {
        return new TokenId(UuidV7.randomUuid());
    }

    public static TokenId of(UUID value) {
        return new TokenId(value);
    }
}
