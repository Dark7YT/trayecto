package com.trayecto.sharing.domain;

/**
 * Estado del acceso compartido.
 * <p>
 * - {@code PENDING}: invitado por el owner, aún sin respuesta del grantee.
 * - {@code ACCEPTED}: grantee aceptó; ya puede ver los viajes COMPLETED del owner.
 * - {@code REJECTED}: grantee declinó. La invitación se conserva para historial.
 * - {@code REVOKED}: el owner anuló el acceso después de haberlo otorgado, o canceló invitación pendiente.
 */
public enum AccessGrantStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    REVOKED;

    public boolean isActive() {
        return this == ACCEPTED;
    }
}
