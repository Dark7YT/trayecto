package com.trayecto.iam.api.events;

import com.trayecto.shared.kernel.UserId;

import java.time.Instant;
import java.util.UUID;

/**
 * Se detectó reuso de un refresh token revocado. Indica robo del token o bug del cliente.
 * Al emitir este evento, toda la familia de tokens del usuario debe revocarse.
 * <p>
 * {@code notifications} envía un correo de alerta al usuario.
 */
public record RefreshTokenReuseDetected(
    UserId userId,
    UUID familyId,
    String deviceFingerprint,
    Instant detectedAt
) {}
