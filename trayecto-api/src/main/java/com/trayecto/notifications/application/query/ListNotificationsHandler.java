package com.trayecto.notifications.application.query;

import com.trayecto.notifications.domain.Notification;
import com.trayecto.notifications.domain.NotificationRepository;
import com.trayecto.shared.kernel.UserId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListNotificationsHandler {

    public record Query(UserId userId, int limit) {}

    private final NotificationRepository repository;

    public ListNotificationsHandler(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Notification> handle(Query query) {
        int limit = Math.max(1, Math.min(query.limit(), 200));
        return repository.findByUserId(query.userId(), limit);
    }
}
