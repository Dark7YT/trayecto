package com.trayecto.sharing.domain;

import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;

import java.util.List;
import java.util.Optional;

public interface AccessGrantRepository {

    Optional<AccessGrant> findById(AccessGrantId id);

    Optional<AccessGrant> findByInviteTokenHash(String hash);

    /** Lista las invitaciones que el owner envió (sin importar status). */
    List<AccessGrant> findSentByOwner(UserId ownerId);

    /** Invitaciones recibidas (resueltas por email del usuario). */
    List<AccessGrant> findByGranteeEmail(Email granteeEmail);

    /** Invitaciones ACEPTADAS donde el usuario es grantee — para listar "owners que me comparten". */
    List<AccessGrant> findActiveAccessFor(UserId granteeId);

    /** Owners cuyos viajes puedo ver. */
    List<UserId> findActiveOwnersFor(UserId granteeId);

    /** True si existe un AccessGrant ACEPTADO entre estos dos usuarios. */
    boolean hasActiveAccess(UserId ownerId, UserId granteeId);

    /** True si ya hay invitación PENDING o ACCEPTED para evitar duplicados. */
    boolean existsActiveInvite(UserId ownerId, Email granteeEmail);

    AccessGrant save(AccessGrant grant);
}
