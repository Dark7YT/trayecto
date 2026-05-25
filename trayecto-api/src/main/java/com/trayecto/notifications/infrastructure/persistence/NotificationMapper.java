package com.trayecto.notifications.infrastructure.persistence;

import com.trayecto.notifications.domain.Notification;
import com.trayecto.notifications.domain.NotificationId;
import com.trayecto.shared.kernel.UserId;

final class NotificationMapper {

    private NotificationMapper() {}

    static NotificationEntity toEntity(Notification n, NotificationEntity reuse) {
        NotificationEntity e = reuse != null ? reuse : new NotificationEntity();
        e.id = n.id().value();
        e.userId = n.userId().value();
        e.type = n.type();
        e.title = n.title();
        e.payload = n.payload();
        e.createdAt = n.createdAt();
        e.readAt = n.readAt().orElse(null);
        return e;
    }

    static Notification toDomain(NotificationEntity e) {
        return Notification.reconstitute(
            NotificationId.of(e.id),
            UserId.of(e.userId),
            e.type,
            e.title,
            e.payload,
            e.createdAt,
            e.readAt
        );
    }
}
