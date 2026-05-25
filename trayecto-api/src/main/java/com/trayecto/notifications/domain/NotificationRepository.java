package com.trayecto.notifications.domain;

import com.trayecto.shared.kernel.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    Optional<Notification> findById(NotificationId id);

    /** Lista las notificaciones del usuario, más recientes primero, limit típico = 50. */
    List<Notification> findByUserId(UserId userId, int limit);

    /** Cuenta no leídas — barato porque hay índice partial WHERE read_at IS NULL. */
    long countUnread(UserId userId);

    int markAllAsRead(UserId userId, Instant readAt);

    Notification save(Notification notification);

    /** Borra notificaciones leídas más viejas que el cutoff. Job programado mensual. */
    int deleteReadOlderThan(Instant cutoff);
}
