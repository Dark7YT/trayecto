package com.trayecto.sharing.infrastructure.persistence;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.sharing.domain.CommentBody;
import com.trayecto.sharing.domain.CommentId;
import com.trayecto.sharing.domain.TripComment;
import com.trayecto.shared.kernel.TripId;

final class TripCommentMapper {

    private TripCommentMapper() {}

    static TripCommentEntity toEntity(TripComment comment, TripCommentEntity reuse) {
        TripCommentEntity e = reuse != null ? reuse : new TripCommentEntity();
        e.id = comment.id().value();
        e.tripId = comment.tripId().value();
        e.tripOwnerId = comment.tripOwnerId().value();
        e.authorId = comment.authorId().value();
        e.body = comment.body().value();
        e.createdAt = comment.createdAt();
        e.editedAt = comment.editedAt().orElse(null);
        e.deletedAt = comment.deletedAt().orElse(null);
        return e;
    }

    static TripComment toDomain(TripCommentEntity e) {
        return TripComment.reconstitute(
            CommentId.of(e.id),
            TripId.of(e.tripId),
            UserId.of(e.tripOwnerId),
            UserId.of(e.authorId),
            new CommentBody(e.body),
            e.createdAt,
            e.editedAt,
            e.deletedAt
        );
    }
}
