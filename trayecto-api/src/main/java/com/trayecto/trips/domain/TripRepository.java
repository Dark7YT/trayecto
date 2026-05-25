package com.trayecto.trips.domain;
import com.trayecto.shared.kernel.TripId;

import com.trayecto.shared.kernel.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TripRepository {

    Optional<Trip> findById(TripId id);

    /** Lista los viajes del usuario (no soft-deleted), ordenados por startedAt desc. */
    List<Trip> findByUser(UserId userId, TripStatus statusFilter, Instant fromInclusive, Instant toExclusive);

    /** Lista solo los completados, para analytics y reportes. */
    List<Trip> findCompletedByUser(UserId userId, Instant fromInclusive, Instant toExclusive);

    /** Lista PENDING + COMPLETED (excluye CANCELLED y soft-deleted), para sharing. */
    List<Trip> findSharedByUser(UserId userId);

    Trip save(Trip trip);

    long countByUserAndStatus(UserId userId, TripStatus status);
}
