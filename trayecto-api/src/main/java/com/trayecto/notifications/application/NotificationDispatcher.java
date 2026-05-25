package com.trayecto.notifications.application;

import com.trayecto.notifications.api.NotificationType;
import com.trayecto.notifications.api.dto.NotificationSnapshot;
import com.trayecto.notifications.domain.Notification;
import com.trayecto.notifications.domain.NotificationRepository;
import com.trayecto.notifications.infrastructure.websocket.NotificationPushService;
import com.trayecto.shared.kernel.UserId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Helper interno que ata el ciclo completo de una notificación:
 * 1) Crea + persiste la Notification (queda en BD para que el usuario la vea al hacer GET).
 * 2) Hace push WebSocket al usuario (si está conectado).
 * <p>
 * El envío de email vive aparte en {@code MailDispatchPort} — cada listener decide si llama
 * a mail según el tipo (ej. una notificación de "comentario añadido" SI requiere email,
 * pero "USER_REGISTERED" no porque ya envía el correo de verificación por otro canal).
 */
@Component
public class NotificationDispatcher {

    private final NotificationRepository repository;
    private final NotificationPushService push;

    public NotificationDispatcher(NotificationRepository repository, NotificationPushService push) {
        this.repository = repository;
        this.push = push;
    }

    @Transactional
    public Notification dispatch(UserId userId, NotificationType type, String title, Map<String, Object> payload) {
        Notification notification = Notification.create(userId, type, title, payload);
        Notification saved = repository.save(notification);

        // Push fuera de la transacción crítica — si falla no rompe el flujo.
        push.push(userId, snapshot(saved));
        return saved;
    }

    private static NotificationSnapshot snapshot(Notification n) {
        return new NotificationSnapshot(
            n.id(), n.userId(), n.type(), n.title(), n.payload(),
            n.createdAt(), n.readAt().orElse(null)
        );
    }
}
