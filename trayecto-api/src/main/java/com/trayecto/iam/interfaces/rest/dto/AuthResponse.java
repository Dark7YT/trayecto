package com.trayecto.iam.interfaces.rest.dto;

import com.trayecto.iam.api.AuthProvider;

import java.util.UUID;

/**
 * Cuerpo de respuesta exitosa para {@code login} y {@code refresh}.
 * El refresh token viaja como cookie HttpOnly aparte, no aquí.
 */
public record AuthResponse(
    String accessToken,
    long expiresInSeconds,
    UserInfo user
) {
    public record UserInfo(
        UUID userId,
        String email,
        String displayName,
        AuthProvider provider,
        boolean emailVerified
    ) {}
}
