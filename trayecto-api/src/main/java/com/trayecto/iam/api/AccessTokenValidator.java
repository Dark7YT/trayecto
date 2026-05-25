package com.trayecto.iam.api;

import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;

import java.util.Optional;

/**
 * Puerto público para validar JWT access tokens emitidos por iam.
 * Lo consume {@code notifications} (en el STOMP CONNECT) y cualquier otro filtro
 * que necesite resolver un Bearer token sin acoplarse al adapter Jwt interno.
 */
public interface AccessTokenValidator {

    Optional<AuthenticatedPrincipal> validate(String token);

    record AuthenticatedPrincipal(UserId userId, Email email) {}
}
