package com.trayecto.notifications.infrastructure.websocket;

import com.trayecto.notifications.api.dto.NotificationSnapshot;
import com.trayecto.shared.kernel.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Envía notificaciones individuales vía WebSocket STOMP al usuario destinatario.
 * El cliente se suscribe a {@code /user/queue/notifications} (Spring lo enruta por sesión).
 */
@Service
public class NotificationPushService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPushService.class);
    private static final String DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationPushService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void push(UserId userId, NotificationSnapshot notification) {
        try {
            messagingTemplate.convertAndSendToUser(userId.asString(), DESTINATION, notification);
            log.debug("Pushed notification {} to user {}",
                notification.id().value(), userId.asString());
        } catch (MessagingException e) {
            // Si el usuario no está conectado, simplemente no recibe el push.
            // La notificación queda persistida y la verá al hacer GET /api/v1/notifications.
            log.debug("Could not push notification to user {} (not connected?): {}",
                userId.asString(), e.getMessage());
        }
    }
}
