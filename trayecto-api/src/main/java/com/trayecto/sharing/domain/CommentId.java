package com.trayecto.sharing.domain;

import com.trayecto.shared.kernel.UuidV7;

import java.util.Objects;
import java.util.UUID;

public record CommentId(UUID value) {

    public CommentId {
        Objects.requireNonNull(value, "CommentId.value must not be null");
    }

    public static CommentId newId() {
        return new CommentId(UuidV7.randomUuid());
    }

    public static CommentId of(UUID value) {
        return new CommentId(value);
    }

    public static CommentId of(String value) {
        return new CommentId(UUID.fromString(value));
    }
}
