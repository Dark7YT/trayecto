package com.trayecto.iam.interfaces.rest.dto;

import com.trayecto.iam.api.AuthProvider;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(
    UUID userId,
    String email,
    String displayName,
    String locale,
    String timezone,
    AuthProvider provider,
    boolean emailVerified,
    Instant createdAt,
    Instant updatedAt
) {}
