package com.trayecto.shared.kernel;

import com.trayecto.shared.kernel.exception.BusinessRuleViolation;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Email validado y normalizado (lowercase, trimmed). Pasa de manera reutilizable
 * entre módulos sin filtrar entidades de iam.
 */
public record Email(String value) {

    // RFC 5322 simplificado y suficientemente estricto para producto. La verificación
    // real ocurre cuando se envía el correo (Brevo) y el usuario hace click.
    private static final Pattern PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$"
    );

    private static final int MAX_LENGTH = 254;

    public Email {
        if (value == null) {
            throw new BusinessRuleViolation("email.required", "Email is required");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new BusinessRuleViolation("email.required", "Email is required");
        }
        if (value.length() > MAX_LENGTH) {
            throw new BusinessRuleViolation("email.too_long",
                "Email must be at most " + MAX_LENGTH + " characters");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new BusinessRuleViolation("email.invalid_format", "Email has invalid format");
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }
}
