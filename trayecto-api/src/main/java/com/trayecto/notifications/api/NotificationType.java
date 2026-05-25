package com.trayecto.notifications.api;

/**
 * Categorías de notificaciones in-app. El payload de cada Notification es un JSON cuyo
 * shape varía según el tipo. El frontend usa el type para renderizar diferente y para
 * routing (link al recurso correspondiente).
 */
public enum NotificationType {
    USER_REGISTERED,            // bienvenida
    EMAIL_VERIFIED,             // confirmación

    PASSWORD_RESET_REQUESTED,   // info para el usuario, no acción
    PASSWORD_CHANGED,           // seguridad

    REFRESH_TOKEN_REUSE,        // alerta de seguridad

    TRIP_COMPLETED,             // viaje cerrado con costo
    BUDGET_WARNING,             // 80% del mes
    BUDGET_EXCEEDED,            // 100% del mes

    ACCESS_GRANT_INVITED,       // recibí invitación a ver viajes de otro
    ACCESS_GRANT_ACCEPTED,      // alguien aceptó mi invitación
    ACCESS_GRANT_REJECTED,      // alguien rechazó mi invitación
    ACCESS_GRANT_REVOKED,       // me revocaron acceso

    COMMENT_ADDED               // comentario nuevo en mi viaje o en uno que comparto
}
