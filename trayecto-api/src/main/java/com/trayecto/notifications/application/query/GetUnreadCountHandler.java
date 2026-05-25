package com.trayecto.notifications.application.query;

import com.trayecto.notifications.domain.NotificationRepository;
import com.trayecto.shared.kernel.UserId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GetUnreadCountHandler {

    public record Query(UserId userId) {}

    private final NotificationRepository repository;

    public GetUnreadCountHandler(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public long handle(Query query) {
        return repository.countUnread(query.userId());
    }
}
