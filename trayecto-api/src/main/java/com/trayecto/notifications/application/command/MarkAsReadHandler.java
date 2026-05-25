package com.trayecto.notifications.application.command;

import com.trayecto.notifications.domain.Notification;
import com.trayecto.notifications.domain.NotificationId;
import com.trayecto.notifications.domain.NotificationRepository;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.shared.kernel.exception.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MarkAsReadHandler {

    public record Command(UserId userId, NotificationId notificationId) {}

    private final NotificationRepository repository;

    public MarkAsReadHandler(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void handle(Command command) {
        Notification notification = repository.findById(command.notificationId())
            .orElseThrow(() -> new NotFoundException("notification.not_found", "Notification not found"));
        if (!notification.userId().equals(command.userId())) {
            throw new BusinessRuleViolation("notification.not_owner",
                "Notification does not belong to current user");
        }
        notification.markAsRead();
        repository.save(notification);
    }
}
