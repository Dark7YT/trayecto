package com.trayecto.shared.kernel.exception;

/**
 * Violación de una invariante de negocio (ej. email mal formado, monto negativo,
 * transición de estado inválida). Mapea a HTTP 422 Unprocessable Entity.
 */
public class BusinessRuleViolation extends DomainException {

    public BusinessRuleViolation(String code, String message) {
        super(code, message);
    }

    public BusinessRuleViolation(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
