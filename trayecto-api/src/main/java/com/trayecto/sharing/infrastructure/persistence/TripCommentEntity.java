package com.trayecto.sharing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "sharing_trip_comments",
    indexes = {
        @Index(name = "ix_sharing_comment_trip", columnList = "trip_id,created_at"),
        @Index(name = "ix_sharing_comment_author", columnList = "author_id")
    }
)
class TripCommentEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "trip_id", nullable = false)
    UUID tripId;

    @Column(name = "trip_owner_id", nullable = false)
    UUID tripOwnerId;

    @Column(name = "author_id", nullable = false)
    UUID authorId;

    @Column(name = "body", nullable = false, length = 2000)
    String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "edited_at")
    Instant editedAt;

    @Column(name = "deleted_at")
    Instant deletedAt;

    TripCommentEntity() {}
}
