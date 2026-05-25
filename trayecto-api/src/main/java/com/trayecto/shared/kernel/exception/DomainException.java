package com.trayecto.shared.kernel.exception;

/**
 * Base de excepciones de dominio. Subclases representan situaciones esperadas
 * (no bugs) que deben mapearse a un status HTTP concreto en el {@code @RestControllerAdvice}.
 * <p>
 * Cada excepción de dominio lleva un {@code code} estable que puede usarse:
 * - en el frontend para i18n del mensaje
 * - en logs estructurados
 * - en el campo {@code type} de un ProblemDetail RFC 7807
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    protected DomainException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
