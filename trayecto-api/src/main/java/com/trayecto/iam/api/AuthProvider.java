package com.trayecto.iam.api;

/**
 * Cómo se autentica el usuario. Es público porque aparece en eventos y DTOs cross-module.
 * <p>
 * - {@code LOCAL}: registrado con email + password (password hash en BD).
 * - {@code GOOGLE}: registrado vía OAuth Google (sin password local).
 * - {@code BOTH}: cuenta local que vinculó Google después (puede usar ambos métodos).
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    BOTH;

    public boolean supportsLocalLogin() {
        return this == LOCAL || this == BOTH;
    }

    public boolean supportsGoogleLogin() {
        return this == GOOGLE || this == BOTH;
    }
}
