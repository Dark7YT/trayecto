package com.trayecto.iam.domain;

import com.trayecto.shared.kernel.exception.BusinessRuleViolation;

/**
 * Nombre de display elegido por el usuario. Visible en perfil y en comentarios.
 */
public record DisplayName(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 50;

    public DisplayName {
        if (value == null) {
            throw new BusinessRuleViolation("display_name.required", "Display name is required");
        }
        value = value.trim();
        if (value.length() < MIN_LENGTH) {
            throw new BusinessRuleViolation("display_name.too_short",
                "Display name must be at least " + MIN_LENGTH + " characters");
        }
        if (value.length() > MAX_LENGTH) {
            throw new BusinessRuleViolation("display_name.too_long",
                "Display name must be at most " + MAX_LENGTH + " characters");
        }
    }
}
