package com.trayecto.notifications.infrastructure.persistence;

import com.trayecto.notifications.domain.Notification;
import com.trayecto.notifications.domain.NotificationId;
import com.trayecto.notifications.domain.NotificationRepository;
import com.trayecto.shared.kernel.UserId;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpa;

    NotificationRepositoryAdapter(NotificationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Notification> findById(NotificationId id) {
        return jpa.findById(id.value()).map(NotificationMapper::toDomain);
    }

    @Override
    public List<Notification> findByUserId(UserId userId, int limit) {
        return jpa.findByUserIdOrderByCreatedAtDesc(userId.value(), Limit.of(limit))
            .stream().map(NotificationMapper::toDomain).toList();
    }

    @Override
    public long countUnread(UserId userId) {
        return jpa.countByUserIdAndReadAtIsNull(userId.value());
    }

    @Override
    @Transactional
    public int markAllAsRead(UserId userId, Instant readAt) {
        return jpa.markAllAsRead(userId.value(), readAt);
    }

    @Override
    public Notification save(Notification notification) {
        NotificationEntity existing = jpa.findById(notification.id().value()).orElse(null);
        NotificationEntity entity = NotificationMapper.toEntity(notification, existing);
        return NotificationMapper.toDomain(jpa.save(entity));
    }

    @Override
    @Transactional
    public int deleteReadOlderThan(Instant cutoff) {
        return jpa.deleteReadOlderThan(cutoff);
    }
}
