package com.trayecto.iam.api;

import com.trayecto.iam.api.dto.UserSnapshot;
import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;

import java.util.Optional;

/**
 * API pública de iam para que otros bounded contexts (trips, sharing, notifications)
 * resuelvan usuarios por id/email sin acoplarse al agregado interno.
 * <p>
 * Solo expone lookups por valor. Operaciones de escritura del usuario (registrar,
 * verificar email, cambiar password) ocurren exclusivamente vía endpoints REST,
 * no como API inter-módulo.
 */
public interface IamPublicApi {

    /** Snapshot del usuario por id. {@code Optional.empty()} si no existe o fue deactivated. */
    Optional<UserSnapshot> findUserSnapshot(UserId userId);

    /** Snapshot del usuario por email (case-insensitive). */
    Optional<UserSnapshot> findUserSnapshotByEmail(Email email);

    /** True si el usuario existe y está ACTIVE. */
    boolean userIsActive(UserId userId);
}
