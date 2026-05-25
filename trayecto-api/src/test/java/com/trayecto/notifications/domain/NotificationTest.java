package com.trayecto.notifications.domain;

import com.trayecto.notifications.api.NotificationType;
import com.trayecto.shared.kernel.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    private static final UserId USER = UserId.newId();

    @Test
    void create_setsDefaults_andIsUnread() {
        Notification n = Notification.create(
            USER, NotificationType.TRIP_COMPLETED, "Viaje cerrado",
            Map.of("tripId", "abc", "totalCost", 42.5)
        );

        assertThat(n.userId()).isEqualTo(USER);
        assertThat(n.type()).isEqualTo(NotificationType.TRIP_COMPLETED);
        assertThat(n.title()).isEqualTo("Viaje cerrado");
        assertThat(n.isUnread()).isTrue();
        assertThat(n.readAt()).isEmpty();
        assertThat(n.payload()).containsEntry("tripId", "abc").containsEntry("totalCost", 42.5);
        assertThat(n.createdAt()).isNotNull();
    }

    @Test
    void create_acceptsNullPayload_storesEmptyMap() {
        Notification n = Notification.create(
            USER, NotificationType.USER_REGISTERED, "Bienvenido", null
        );

        assertThat(n.payload()).isEmpty();
    }

    @Test
    void payload_isImmutable() {
        Notification n = Notification.create(
            USER, NotificationType.COMMENT_ADDED, "Nuevo comentario",
            Map.of("preview", "Hola")
        );

        // Map.copyOf produces an immutable map; mutating attempts throw.
        assertThatThrownBy(() -> n.payload().put("hacked", true))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void markAsRead_setsReadAt_andMarksRead() {
        Notification n = Notification.create(
            USER, NotificationType.BUDGET_WARNING, "80% presupuesto", Map.of()
        );

        n.markAsRead();

        assertThat(n.isUnread()).isFalse();
        assertThat(n.readAt()).isPresent();
    }

    @Test
    void markAsRead_isIdempotent_doesNotResetReadAt() {
        Notification n = Notification.create(
            USER, NotificationType.BUDGET_WARNING, "80% presupuesto", Map.of()
        );

        n.markAsRead();
        Instant firstRead = n.readAt().orElseThrow();

        // Pequeña pausa simulada vía reconstitute es exagerado; reaplicar markAsRead
        // debe ser no-op: el readAt no debe cambiar.
        n.markAsRead();

        assertThat(n.readAt()).hasValue(firstRead);
    }

    @Test
    void reconstitute_preservesAllFields() {
        NotificationId id = NotificationId.newId();
        Instant created = Instant.parse("2026-05-23T10:00:00Z");
        Instant read = Instant.parse("2026-05-23T10:15:00Z");

        Notification n = Notification.reconstitute(
            id, USER, NotificationType.ACCESS_GRANT_INVITED,
            "Invitación", Map.of("ownerEmail", "papa@example.com"),
            created, read
        );

        assertThat(n.id()).isEqualTo(id);
        assertThat(n.userId()).isEqualTo(USER);
        assertThat(n.type()).isEqualTo(NotificationType.ACCESS_GRANT_INVITED);
        assertThat(n.title()).isEqualTo("Invitación");
        assertThat(n.payload()).containsEntry("ownerEmail", "papa@example.com");
        assertThat(n.createdAt()).isEqualTo(created);
        assertThat(n.readAt()).hasValue(read);
        assertThat(n.isUnread()).isFalse();
    }

    @Test
    void reconstitute_withNullReadAt_isUnread() {
        Notification n = Notification.reconstitute(
            NotificationId.newId(), USER, NotificationType.COMMENT_ADDED,
            "Comentario nuevo", Map.of(),
            Instant.now(), null
        );

        assertThat(n.isUnread()).isTrue();
        assertThat(n.readAt()).isEmpty();
    }
}
