package com.trayecto.iam.interfaces.rest;

import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.shared.kernel.exception.DomainException;
import com.trayecto.shared.kernel.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mapea excepciones de dominio y de validación a ProblemDetail (RFC 7807).
 * <p>
 * Convenio:
 * - {@code type}: URI relativa {@code /errors/<code>} (estable, para i18n del frontend).
 * - {@code title}: clase del error (human-readable).
 * - {@code detail}: mensaje técnico.
 * - {@code errors}: solo en validación, mapa campo → mensaje.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.code(), "Not Found", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(BusinessRuleViolation.class)
    ResponseEntity<ProblemDetail> businessRule(BusinessRuleViolation ex) {
        // Mapping fino por code para devolver el status correcto en casos comunes.
        HttpStatus status = switch (ex.code()) {
            case "user.email_already_registered" -> HttpStatus.CONFLICT;
            case "auth.invalid_credentials",
                 "auth.refresh_token_unknown",
                 "auth.refresh_token_expired",
                 "auth.refresh_token_reuse",
                 "auth.account_not_active",
                 "auth.account_deactivated",
                 "auth.email_not_verified",
                 "auth.use_google_login",
                 "auth.invalid_current_password" -> HttpStatus.UNAUTHORIZED;
            case "auth.refresh_token_missing" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return problem(status, ex.code(), status.getReasonPhrase(), ex.getMessage(), Map.of());
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> domain(DomainException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.code(), "Domain Error", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
            fieldErrors.put(fe.getField(), fe.getDefaultMessage())
        );
        return problem(HttpStatus.BAD_REQUEST, "validation.failed",
            "Validation Failed", "One or more fields failed validation",
            Map.of("errors", fieldErrors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> illegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "request.invalid",
            "Bad Request", ex.getMessage(), Map.of());
    }

    /**
     * Catch-all para excepciones no manejadas. Sin esto, Spring hace forward a
     * /error que (al estar protegido) devuelve 401 confuso para el cliente.
     * <p>
     * Seguridad: NO exponemos {@code ex.getMessage()} al cliente — puede filtrar
     * paths internos, fragmentos SQL, JWT claims, etc. Solo devolvemos un mensaje
     * genérico + un {@code traceId} (timestamp + clase) para correlacionar con
     * los logs internos donde sí va el stack trace completo.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unhandled(Exception ex) {
        String traceId = System.currentTimeMillis() + "-" + ex.getClass().getSimpleName();
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
            .error("Unhandled exception [traceId={}]", traceId, ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "server.unhandled",
            "Internal Server Error",
            "Ocurrió un error inesperado. Si el problema persiste, contacta soporte.",
            Map.of("traceId", traceId));
    }

    private static ResponseEntity<ProblemDetail> problem(
        HttpStatus status, String code, String title, String detail, Map<String, Object> extras
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("/errors/" + code));
        Map<String, Object> properties = new HashMap<>(extras);
        properties.put("code", code);
        properties.forEach(pd::setProperty);
        return ResponseEntity.status(status).body(pd);
    }
}
