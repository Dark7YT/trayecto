package com.trayecto.iam.domain;

/**
 * Ciclo de vida de la cuenta.
 * <p>
 * - {@code PENDING_VERIFICATION}: usuario registrado pero no ha clickeado el link del email.
 *   No puede hacer login local. Solo se mantiene 30 días antes de purgarse.
 * - {@code ACTIVE}: cuenta funcional, puede usar todos los flujos.
 * - {@code DEACTIVATED}: el usuario eliminó su cuenta. Se preserva el row para integridad
 *   referencial de viajes/comentarios. No puede hacer login.
 */
public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    DEACTIVATED
}
