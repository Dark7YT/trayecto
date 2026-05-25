package com.trayecto.iam.api.dto;

import com.trayecto.iam.api.AuthProvider;
import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;

/**
 * Vista inmutable de un usuario para consumo cross-module. Los demás bounded contexts
 * (trips, sharing, notifications) reciben este snapshot por valor y nunca acceden
 * al agregado User directamente.
 */
public record UserSnapshot(
    UserId id,
    Email email,
    String displayName,
    AuthProvider provider,
    boolean emailVerified
) {}
