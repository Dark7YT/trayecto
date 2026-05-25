package com.trayecto.shared.kernel.exception;

/**
 * Recurso no encontrado por id, email u otro identificador. Mapea a HTTP 404.
 */
public class NotFoundException extends DomainException {

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
