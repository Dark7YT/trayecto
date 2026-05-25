package com.trayecto.trips.domain;
import com.trayecto.shared.kernel.TripId;

import com.trayecto.shared.kernel.Kilometers;
import com.trayecto.shared.kernel.Money;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.trips.api.events.TripCancelled;
import com.trayecto.trips.api.events.TripCompleted;
import com.trayecto.trips.api.events.TripEdited;
import com.trayecto.trips.api.events.TripStarted;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Agregado raíz Trip. Modelo del flujo:
 * <pre>
 *   start(startKm, photo)  →  PENDING
 *      ↓                       ↓
 *   complete(endKm,         cancel()  →  CANCELLED
 *   multiplierId, photo)
 *      ↓
 *   COMPLETED (costo congelado)
 * </pre>
 * Edición permitida en cualquier estado (auditada). El soft-delete usa {@code deletedAt}.
 * <p>
 * El {@code multiplierRate} se guarda como snapshot al COMPLETE: si el usuario cambia el
 * value del UserMultiplier después, el costo del viaje no se recalcula.
 */
public final class Trip {

    private final TripId id;
    private final UserId userId;
    private TripName name;
    private Kilometers startKm;
    private Kilometers endKm;             // null si PENDING
    private MultiplierId multiplierId;    // null si PENDING
    private BigDecimal multiplierRate;    // snapshot del rate, null si PENDING
    private Money totalCost;              // null si PENDING/CANCELLED
    private TripStatus status;
    private String startPhotoUrl;         // null si no se subió
    private String endPhotoUrl;
    private final Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private final List<Object> domainEvents = new ArrayList<>();

    private Trip(
        TripId id, UserId userId, TripName name,
        Kilometers startKm, Kilometers endKm,
        MultiplierId multiplierId, BigDecimal multiplierRate, Money totalCost,
        TripStatus status,
        String startPhotoUrl, String endPhotoUrl,
        Instant startedAt, Instant completedAt, Instant cancelledAt,
        Instant updatedAt, Instant deletedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.name = Objects.requireNonNull(name);
        this.startKm = Objects.requireNonNull(startKm);
        this.endKm = endKm;
        this.multiplierId = multiplierId;
        this.multiplierRate = multiplierRate;
        this.totalCost = totalCost;
        this.status = Objects.requireNonNull(status);
        this.startPhotoUrl = startPhotoUrl;
        this.endPhotoUrl = endPhotoUrl;
        this.startedAt = Objects.requireNonNull(startedAt);
        this.completedAt = completedAt;
        this.cancelledAt = cancelledAt;
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.deletedAt = deletedAt;
    }

    // ============ Factories ============

    public static Trip start(UserId userId, TripName name, Kilometers startKm, String startPhotoUrl) {
        Instant now = Instant.now();
        Trip trip = new Trip(
            TripId.newId(), userId, name,
            startKm, null, null, null, null,
            TripStatus.PENDING,
            startPhotoUrl, null,
            now, null, null,
            now, null
        );
        trip.raise(new TripStarted(trip.id, userId, name.value(), startKm.value(), now));
        return trip;
    }

    public static Trip reconstitute(
        TripId id, UserId userId, TripName name,
        Kilometers startKm, Kilometers endKm,
        MultiplierId multiplierId, BigDecimal multiplierRate, Money totalCost,
        TripStatus status,
        String startPhotoUrl, String endPhotoUrl,
        Instant startedAt, Instant completedAt, Instant cancelledAt,
        Instant updatedAt, Instant deletedAt
    ) {
        return new Trip(id, userId, name, startKm, endKm, multiplierId, multiplierRate, totalCost,
            status, startPhotoUrl, endPhotoUrl, startedAt, completedAt, cancelledAt, updatedAt, deletedAt);
    }

    // ============ Comportamiento ============

    public void complete(Kilometers endKm, MultiplierId multiplierId, BigDecimal multiplierRate, String endPhotoUrl) {
        requireNotDeleted();
        if (status == TripStatus.CANCELLED) {
            throw new BusinessRuleViolation("trip.cancelled_cannot_complete",
                "Cancelled trips cannot be completed");
        }
        if (status == TripStatus.COMPLETED) {
            throw new BusinessRuleViolation("trip.already_completed",
                "Trip is already completed; use edit to adjust values");
        }
        Objects.requireNonNull(endKm);
        Objects.requireNonNull(multiplierId);
        Objects.requireNonNull(multiplierRate);
        if (multiplierRate.signum() <= 0) {
            throw new BusinessRuleViolation("trip.multiplier_rate_invalid",
                "Multiplier rate must be positive");
        }
        Kilometers distance = this.startKm.distanceTo(endKm);

        this.endKm = endKm;
        this.multiplierId = multiplierId;
        this.multiplierRate = multiplierRate;
        this.endPhotoUrl = endPhotoUrl;
        this.totalCost = Money.pen(distance.value().multiply(multiplierRate));
        this.status = TripStatus.COMPLETED;
        Instant now = Instant.now();
        this.completedAt = now;
        this.updatedAt = now;
        raise(new TripCompleted(
            id, userId, multiplierId.value(),
            distance.value(), totalCost, multiplierRate, now
        ));
    }

    public void cancel() {
        requireNotDeleted();
        if (status == TripStatus.COMPLETED) {
            throw new BusinessRuleViolation("trip.completed_cannot_cancel",
                "Completed trips cannot be cancelled");
        }
        if (status == TripStatus.CANCELLED) return; // idempotente
        this.status = TripStatus.CANCELLED;
        Instant now = Instant.now();
        this.cancelledAt = now;
        this.updatedAt = now;
        raise(new TripCancelled(id, userId, now));
    }

    /**
     * Edita el viaje. Permite cambiar nombre y km incluso si COMPLETED (caso de uso real:
     * el usuario se dio cuenta tarde que escribió mal el km final). Si está COMPLETED y se
     * cambia algún km, se recalcula el totalCost con el mismo multiplierRate snapshot.
     */
    public void edit(TripName newName, Kilometers newStartKm, Kilometers newEndKm) {
        requireNotDeleted();
        Objects.requireNonNull(newName);
        Objects.requireNonNull(newStartKm);

        this.name = newName;
        this.startKm = newStartKm;
        if (newEndKm != null) {
            this.endKm = newEndKm;
        }

        if (status == TripStatus.COMPLETED && this.endKm != null) {
            Kilometers distance = this.startKm.distanceTo(this.endKm);
            this.totalCost = Money.pen(distance.value().multiply(multiplierRate));
        }
        this.updatedAt = Instant.now();
        raise(new TripEdited(id, userId, updatedAt));
    }

    public void softDelete() {
        if (deletedAt != null) return;
        this.deletedAt = Instant.now();
        this.updatedAt = deletedAt;
    }

    private void requireNotDeleted() {
        if (deletedAt != null) {
            throw new BusinessRuleViolation("trip.deleted",
                "Trip has been deleted and cannot be modified");
        }
    }

    private void raise(Object event) {
        domainEvents.add(event);
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    // ============ Queries ============

    public boolean isPending() { return status == TripStatus.PENDING; }
    public boolean isCompleted() { return status == TripStatus.COMPLETED; }
    public boolean isCancelled() { return status == TripStatus.CANCELLED; }
    public boolean isDeleted() { return deletedAt != null; }

    // ============ Getters ============

    public TripId id() { return id; }
    public UserId userId() { return userId; }
    public TripName name() { return name; }
    public Kilometers startKm() { return startKm; }
    public Optional<Kilometers> endKm() { return Optional.ofNullable(endKm); }
    public Optional<MultiplierId> multiplierId() { return Optional.ofNullable(multiplierId); }
    public Optional<BigDecimal> multiplierRate() { return Optional.ofNullable(multiplierRate); }
    public Optional<Money> totalCost() { return Optional.ofNullable(totalCost); }
    public TripStatus status() { return status; }
    public Optional<String> startPhotoUrl() { return Optional.ofNullable(startPhotoUrl); }
    public Optional<String> endPhotoUrl() { return Optional.ofNullable(endPhotoUrl); }
    public Instant startedAt() { return startedAt; }
    public Optional<Instant> completedAt() { return Optional.ofNullable(completedAt); }
    public Optional<Instant> cancelledAt() { return Optional.ofNullable(cancelledAt); }
    public Instant updatedAt() { return updatedAt; }
    public Optional<Instant> deletedAt() { return Optional.ofNullable(deletedAt); }
}
