package com.trayecto.notifications.api.dto;

import com.trayecto.notifications.api.NotificationType;
import com.trayecto.notifications.domain.NotificationId;
import com.trayecto.shared.kernel.UserId;

import java.time.Instant;
import java.util.Map;

public record NotificationSnapshot(
    NotificationId id,
    UserId userId,
    NotificationType type,
    String title,
    Map<String, Object> payload,
    Instant createdAt,
    Instant readAt
) {}
