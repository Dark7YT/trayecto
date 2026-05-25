package com.trayecto.sharing.api;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.sharing.api.dto.AccessGrantSnapshot;
import com.trayecto.shared.kernel.TripId;

import java.util.List;

/**
 * API pública del módulo sharing. {@code notifications} consume para resolver destinatarios
 * de los emails de comentario.
 */
public interface SharingPublicApi {

    /** True si {@code viewerId} puede ver los viajes COMPLETED de {@code ownerId}. */
    boolean canViewTripsOf(UserId viewerId, UserId ownerId);

    /** Lista los granteeIds que tienen acceso ACEPTADO a los viajes de owner. */
    List<UserId> findActiveGranteesOf(UserId ownerId);

    /** Lista de invitaciones aceptadas donde el usuario es grantee — para resolver owners. */
    List<AccessGrantSnapshot> findActiveGrantsFor(UserId granteeId);

    /** True si el comentador tiene acceso para escribir en este viaje. */
    boolean canCommentOnTrip(UserId userId, TripId tripId, UserId tripOwnerId);
}
