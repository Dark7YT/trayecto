package com.trayecto.notifications.domain;

import com.trayecto.shared.kernel.UuidV7;

import java.util.Objects;
import java.util.UUID;

public record NotificationId(UUID value) {

    public NotificationId {
        Objects.requireNonNull(value);
    }

    public static NotificationId newId() {
        return new NotificationId(UuidV7.randomUuid());
    }

    public static NotificationId of(UUID value) {
        return new NotificationId(value);
    }
}
