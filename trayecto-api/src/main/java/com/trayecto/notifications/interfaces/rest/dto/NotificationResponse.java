package com.trayecto.notifications.interfaces.rest.dto;

import com.trayecto.notifications.api.NotificationType;
import com.trayecto.notifications.domain.Notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    NotificationType type,
    String title,
    Map<String, Object> payload,
    Instant createdAt,
    Instant readAt,
    boolean unread
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
            n.id().value(), n.type(), n.title(), n.payload(),
            n.createdAt(), n.readAt().orElse(null), n.isUnread()
        );
    }
}
