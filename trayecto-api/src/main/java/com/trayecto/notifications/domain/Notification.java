package com.trayecto.notifications.domain;

import com.trayecto.notifications.api.NotificationType;
import com.trayecto.shared.kernel.UserId;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Notificación in-app dirigida a un usuario. El {@code payload} es un mapa heterogéneo
 * que el frontend interpreta según el {@code type} (ej. para COMMENT_ADDED contiene
 * {tripId, tripName, authorName, preview}).
 * <p>
 * Idempotencia: {@code markAsRead} es segura si ya está leída — no emite eventos
 * duplicados ni cambia el {@code readAt} original.
 */
public final class Notification {

    private final NotificationId id;
    private final UserId userId;
    private final NotificationType type;
    private final String title;
    private final Map<String, Object> payload;
    private final Instant createdAt;
    private Instant readAt;

    private Notification(
        NotificationId id, UserId userId, NotificationType type,
        String title, Map<String, Object> payload,
        Instant createdAt, Instant readAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.type = Objects.requireNonNull(type);
        this.title = Objects.requireNonNull(title);
        this.payload = Map.copyOf(payload == null ? Map.of() : payload);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.readAt = readAt;
    }

    public static Notification create(
        UserId userId, NotificationType type, String title, Map<String, Object> payload
    ) {
        return new Notification(
            NotificationId.newId(), userId, type, title, payload,
            Instant.now(), null
        );
    }

    public static Notification reconstitute(
        NotificationId id, UserId userId, NotificationType type,
        String title, Map<String, Object> payload,
        Instant createdAt, Instant readAt
    ) {
        return new Notification(id, userId, type, title, payload, createdAt, readAt);
    }

    public void markAsRead() {
        if (readAt != null) return;
        this.readAt = Instant.now();
    }

    public boolean isUnread() {
        return readAt == null;
    }

    public NotificationId id() { return id; }
    public UserId userId() { return userId; }
    public NotificationType type() { return type; }
    public String title() { return title; }
    public Map<String, Object> payload() { return payload; }
    public Instant createdAt() { return createdAt; }
    public Optional<Instant> readAt() { return Optional.ofNullable(readAt); }
}
