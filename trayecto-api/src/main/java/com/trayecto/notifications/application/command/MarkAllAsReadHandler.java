package com.trayecto.notifications.application.command;

import com.trayecto.notifications.domain.NotificationRepository;
import com.trayecto.shared.kernel.UserId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class MarkAllAsReadHandler {

    public record Command(UserId userId) {}
    public record Result(int updated) {}

    private final NotificationRepository repository;

    public MarkAllAsReadHandler(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Result handle(Command command) {
        int updated = repository.markAllAsRead(command.userId(), Instant.now());
        return new Result(updated);
    }
}
