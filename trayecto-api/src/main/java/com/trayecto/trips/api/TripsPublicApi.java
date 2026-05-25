package com.trayecto.trips.api;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.api.dto.MultiplierSnapshot;
import com.trayecto.trips.api.dto.TripSnapshot;
import com.trayecto.shared.kernel.TripId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * API pública del módulo trips. Consumidores: {@code sharing} (ver viajes que otros
 * comparten) y {@code analytics} (agregaciones, reportes). Las operaciones de
 * escritura solo ocurren vía REST endpoints, no como API inter-módulo.
 */
public interface TripsPublicApi {

    Optional<TripSnapshot> findTripSnapshot(TripId tripId);

    /** Solo COMPLETED, no deleted. Para analytics y reportes (donde solo cobramos cerrados). */
    List<TripSnapshot> listCompletedByUser(UserId ownerId, Instant fromInclusive, Instant toExclusive);

    /** PENDING + COMPLETED (excluye CANCELLED y deleted). Para sharing — el grantee
     *  puede ver lo que está en curso y lo cerrado, pero no lo cancelado. */
    List<TripSnapshot> listSharedByUser(UserId ownerId);

    /**
     * Lista todos los multiplicadores definidos por el usuario. Para analytics —
     * resolver nombre legible al agrupar viajes por multiplierId.
     */
    List<MultiplierSnapshot> listMultipliersByUser(UserId userId);

    /** True si el viaje existe, no está deleted y pertenece al userId. */
    boolean isOwnedBy(TripId tripId, UserId userId);
}
