package com.trayecto.notifications.interfaces.rest;

import com.trayecto.notifications.application.command.MarkAllAsReadHandler;
import com.trayecto.notifications.application.command.MarkAsReadHandler;
import com.trayecto.notifications.application.query.GetUnreadCountHandler;
import com.trayecto.notifications.application.query.ListNotificationsHandler;
import com.trayecto.notifications.domain.NotificationId;
import com.trayecto.notifications.interfaces.rest.dto.NotificationResponse;
import com.trayecto.shared.kernel.UserId;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
class NotificationController {

    private final ListNotificationsHandler listHandler;
    private final GetUnreadCountHandler unreadCountHandler;
    private final MarkAsReadHandler markAsReadHandler;
    private final MarkAllAsReadHandler markAllAsReadHandler;

    NotificationController(
        ListNotificationsHandler listHandler,
        GetUnreadCountHandler unreadCountHandler,
        MarkAsReadHandler markAsReadHandler,
        MarkAllAsReadHandler markAllAsReadHandler
    ) {
        this.listHandler = listHandler;
        this.unreadCountHandler = unreadCountHandler;
        this.markAsReadHandler = markAsReadHandler;
        this.markAllAsReadHandler = markAllAsReadHandler;
    }

    @GetMapping
    ResponseEntity<List<NotificationResponse>> list(
        @AuthenticationPrincipal UserId userId,
        @RequestParam(defaultValue = "50") int limit
    ) {
        requireAuth(userId);
        return ResponseEntity.ok(
            listHandler.handle(new ListNotificationsHandler.Query(userId, limit))
                .stream().map(NotificationResponse::from).toList()
        );
    }

    @GetMapping("/unread-count")
    ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal UserId userId) {
        requireAuth(userId);
        long count = unreadCountHandler.handle(new GetUnreadCountHandler.Query(userId));
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{id}/read")
    ResponseEntity<Void> markAsRead(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID id
    ) {
        requireAuth(userId);
        markAsReadHandler.handle(new MarkAsReadHandler.Command(userId, NotificationId.of(id)));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    ResponseEntity<Map<String, Integer>> markAllAsRead(@AuthenticationPrincipal UserId userId) {
        requireAuth(userId);
        var result = markAllAsReadHandler.handle(new MarkAllAsReadHandler.Command(userId));
        return ResponseEntity.ok(Map.of("updated", result.updated()));
    }

    private static void requireAuth(UserId userId) {
        if (userId == null) throw new AccessDeniedException("Authentication required");
    }
}
