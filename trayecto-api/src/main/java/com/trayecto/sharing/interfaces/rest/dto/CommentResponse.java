package com.trayecto.sharing.interfaces.rest.dto;

import com.trayecto.sharing.domain.TripComment;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    UUID tripId,
    UUID authorId,
    String authorDisplayName,
    String body,
    Instant createdAt,
    Instant editedAt
) {
    public static CommentResponse from(TripComment comment, String authorDisplayName) {
        return new CommentResponse(
            comment.id().value(),
            comment.tripId().value(),
            comment.authorId().value(),
            authorDisplayName,
            comment.body().value(),
            comment.createdAt(),
            comment.editedAt().orElse(null)
        );
    }
}
