package com.trayecto.sharing.domain;

import com.trayecto.shared.kernel.TripId;

import java.util.List;
import java.util.Optional;

public interface TripCommentRepository {

    Optional<TripComment> findById(CommentId id);

    /** Lista los comentarios visibles (no soft-deleted) de un viaje, ordenados por createdAt asc. */
    List<TripComment> findByTrip(TripId tripId);

    long countByTrip(TripId tripId);

    TripComment save(TripComment comment);
}
