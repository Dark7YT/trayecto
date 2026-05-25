package com.trayecto.iam.domain;

import com.trayecto.shared.kernel.exception.BusinessRuleViolation;

/**
 * Hash bcrypt de la contraseña. El valor opaco se persiste en BD; el plaintext nunca.
 * Validamos el formato bcrypt para detectar bugs (ej. olvidar de hashear antes de persistir).
 */
public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolation("password_hash.required",
                "PasswordHash value must not be blank");
        }
        if (!isBcryptFormat(value)) {
            throw new BusinessRuleViolation("password_hash.invalid_format",
                "PasswordHash must be in bcrypt format ($2a$/$2b$/$2y$)");
        }
    }

    private static boolean isBcryptFormat(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
