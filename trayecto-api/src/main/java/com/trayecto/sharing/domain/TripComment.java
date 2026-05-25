package com.trayecto.sharing.domain;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.sharing.api.events.CommentAdded;
import com.trayecto.sharing.api.events.CommentDeleted;
import com.trayecto.sharing.api.events.CommentEdited;
import com.trayecto.shared.kernel.TripId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Comentario sobre un viaje. La autorización (autor debe ser owner del trip o grantee con
 * acceso ACEPTADO) la valida el CommandHandler, no el aggregate.
 */
public final class TripComment {

    private static final int PREVIEW_MAX = 200;

    private final CommentId id;
    private final TripId tripId;
    private final UserId tripOwnerId;   // se snapshot para el evento (notifications no necesita resolver)
    private final UserId authorId;
    private CommentBody body;
    private final Instant createdAt;
    private Instant editedAt;
    private Instant deletedAt;

    private final List<Object> domainEvents = new ArrayList<>();

    private TripComment(
        CommentId id, TripId tripId, UserId tripOwnerId, UserId authorId,
        CommentBody body, Instant createdAt, Instant editedAt, Instant deletedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.tripId = Objects.requireNonNull(tripId);
        this.tripOwnerId = Objects.requireNonNull(tripOwnerId);
        this.authorId = Objects.requireNonNull(authorId);
        this.body = Objects.requireNonNull(body);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.editedAt = editedAt;
        this.deletedAt = deletedAt;
    }

    public static TripComment create(TripId tripId, UserId tripOwnerId, UserId authorId, CommentBody body) {
        Instant now = Instant.now();
        TripComment comment = new TripComment(
            CommentId.newId(), tripId, tripOwnerId, authorId, body, now, null, null
        );
        comment.raise(new CommentAdded(
            comment.id.value(), tripId, tripOwnerId, authorId, preview(body.value()), now
        ));
        return comment;
    }

    public static TripComment reconstitute(
        CommentId id, TripId tripId, UserId tripOwnerId, UserId authorId,
        CommentBody body, Instant createdAt, Instant editedAt, Instant deletedAt
    ) {
        return new TripComment(id, tripId, tripOwnerId, authorId, body, createdAt, editedAt, deletedAt);
    }

    public void edit(UserId editorId, CommentBody newBody) {
        requireNotDeleted();
        if (!editorId.equals(authorId)) {
            throw new BusinessRuleViolation("comment.not_author", "Only the author can edit this comment");
        }
        Instant now = Instant.now();
        this.body = Objects.requireNonNull(newBody);
        this.editedAt = now;
        raise(new CommentEdited(id.value(), tripId, authorId, now));
    }

    public void softDelete(UserId deleterId) {
        if (deletedAt != null) return;
        if (!deleterId.equals(authorId) && !deleterId.equals(tripOwnerId)) {
            throw new BusinessRuleViolation("comment.not_authorized_delete",
                "Only the author or the trip owner can delete this comment");
        }
        Instant now = Instant.now();
        this.deletedAt = now;
        raise(new CommentDeleted(id.value(), tripId, authorId, now));
    }

    private void requireNotDeleted() {
        if (deletedAt != null) {
            throw new BusinessRuleViolation("comment.deleted",
                "Comment has been deleted and cannot be modified");
        }
    }

    private static String preview(String body) {
        return body.length() > PREVIEW_MAX ? body.substring(0, PREVIEW_MAX) + "…" : body;
    }

    private void raise(Object event) {
        domainEvents.add(event);
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public boolean isDeleted() { return deletedAt != null; }

    // Getters
    public CommentId id() { return id; }
    public TripId tripId() { return tripId; }
    public UserId tripOwnerId() { return tripOwnerId; }
    public UserId authorId() { return authorId; }
    public CommentBody body() { return body; }
    public Instant createdAt() { return createdAt; }
    public Optional<Instant> editedAt() { return Optional.ofNullable(editedAt); }
    public Optional<Instant> deletedAt() { return Optional.ofNullable(deletedAt); }
}
