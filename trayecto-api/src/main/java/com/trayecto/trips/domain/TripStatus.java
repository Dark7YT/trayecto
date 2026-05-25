package com.trayecto.trips.domain;

/**
 * Ciclo de vida del viaje.
 * <p>
 * - {@code PENDING}: el usuario inició el viaje (foto+km inicial) pero no lo ha cerrado.
 *   El costo todavía no se conoce.
 * - {@code COMPLETED}: el viaje se cerró con km final + multiplicador. Costo congelado.
 * - {@code CANCELLED}: el viaje fue anulado por el usuario. No suma a presupuesto.
 * <p>
 * Las transiciones permitidas son PENDING → COMPLETED y PENDING → CANCELLED.
 * Una vez COMPLETED o CANCELLED, el viaje puede editarse pero no cambia de estado.
 */
public enum TripStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}
