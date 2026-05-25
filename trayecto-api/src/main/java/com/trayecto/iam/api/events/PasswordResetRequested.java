package com.trayecto.iam.api.events;

import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;

import java.time.Instant;

/**
 * Se solicitó reset de password. {@code notifications} envía el correo con el link
 * que contiene el token raw. El token raw va en el evento (no hashed) porque solo
 * existe en este momento — luego solo se persiste el hash.
 */
public record PasswordResetRequested(
    UserId userId,
    Email email,
    String resetTokenRaw,
    Instant requestedAt
) {}
