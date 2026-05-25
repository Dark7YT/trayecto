package com.trayecto.notifications.infrastructure.persistence;

import com.trayecto.notifications.api.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
    name = "notifications_notifications",
    indexes = {
        @Index(name = "ix_notif_user_created", columnList = "user_id,created_at"),
        @Index(name = "ix_notif_user_unread", columnList = "user_id")
    }
)
class NotificationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    String title;

    /** Postgres jsonb. Hibernate 6 sabe mapear Map<String,Object> con JdbcTypeCode JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    Map<String, Object> payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "read_at")
    Instant readAt;

    NotificationEntity() {}
}
