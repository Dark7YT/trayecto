package com.trayecto.iam.api.events;

import com.trayecto.iam.api.AuthProvider;
import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;

import java.time.Instant;

/**
 * Un usuario fue registrado. {@code notifications} escucha para enviar correo de verificación
 * (cuando provider = LOCAL) o de bienvenida (cuando provider = GOOGLE, ya pre-verificado).
 */
public record UserRegistered(
    UserId userId,
    Email email,
    String displayName,
    AuthProvider provider,
    String emailVerificationTokenRaw,
    Instant registeredAt
) {}
