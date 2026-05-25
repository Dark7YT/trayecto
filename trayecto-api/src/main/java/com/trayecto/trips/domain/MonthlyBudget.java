package com.trayecto.trips.domain;

import com.trayecto.shared.kernel.Money;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.UuidV7;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.trips.api.events.BudgetThresholdReached;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Presupuesto mensual del usuario. El {@code currentSpend} se actualiza cuando un viaje se
 * cierra; al cruzar 80% o 100% emite {@link BudgetThresholdReached} (cada umbral 1 vez por mes).
 * <p>
 * Identidad lógica: (userId, year, month). El UUID es interno para el agregado.
 */
public final class MonthlyBudget {

    public static final int THRESHOLD_WARNING_PERCENT = 80;
    public static final int THRESHOLD_EXCEEDED_PERCENT = 100;

    private final UUID id;
    private final UserId userId;
    private final YearMonth period;
    private Money amount;
    private Money currentSpend;
    private boolean warningSent;
    private boolean exceededSent;
    private final Instant createdAt;
    private Instant updatedAt;

    private final List<Object> domainEvents = new ArrayList<>();

    private MonthlyBudget(
        UUID id, UserId userId, YearMonth period, Money amount, Money currentSpend,
        boolean warningSent, boolean exceededSent,
        Instant createdAt, Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.period = Objects.requireNonNull(period);
        this.amount = validateAmount(amount);
        this.currentSpend = Objects.requireNonNullElse(currentSpend, Money.zeroPen());
        this.warningSent = warningSent;
        this.exceededSent = exceededSent;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static MonthlyBudget create(UserId userId, YearMonth period, Money amount) {
        Instant now = Instant.now();
        return new MonthlyBudget(UuidV7.randomUuid(), userId, period, amount, Money.zeroPen(),
            false, false, now, now);
    }

    public static MonthlyBudget reconstitute(
        UUID id, UserId userId, YearMonth period, Money amount, Money currentSpend,
        boolean warningSent, boolean exceededSent,
        Instant createdAt, Instant updatedAt
    ) {
        return new MonthlyBudget(id, userId, period, amount, currentSpend, warningSent, exceededSent,
            createdAt, updatedAt);
    }

    /** Suma el costo de un viaje recién completado al gasto del mes y dispara umbrales. */
    public void recordSpend(Money tripCost) {
        Objects.requireNonNull(tripCost);
        if (tripCost.isNegative()) {
            throw new BusinessRuleViolation("budget.spend_negative", "Spend cannot be negative");
        }
        this.currentSpend = this.currentSpend.plus(tripCost);
        this.updatedAt = Instant.now();
        checkThresholds();
    }

    /** Actualiza el monto del presupuesto sin tocar el spend acumulado. */
    public void updateAmount(Money newAmount) {
        this.amount = validateAmount(newAmount);
        this.updatedAt = Instant.now();
        // Recalcular flags por si el nuevo amount es menor.
        if (currentSpend.amount().compareTo(percentOf(THRESHOLD_WARNING_PERCENT)) < 0) {
            warningSent = false;
        }
        if (currentSpend.amount().compareTo(percentOf(THRESHOLD_EXCEEDED_PERCENT)) < 0) {
            exceededSent = false;
        }
    }

    private void checkThresholds() {
        if (!warningSent && currentSpend.amount().compareTo(percentOf(THRESHOLD_WARNING_PERCENT)) >= 0) {
            warningSent = true;
            raise(new BudgetThresholdReached(userId, period, THRESHOLD_WARNING_PERCENT,
                currentSpend, amount, updatedAt));
        }
        if (!exceededSent && currentSpend.amount().compareTo(percentOf(THRESHOLD_EXCEEDED_PERCENT)) >= 0) {
            exceededSent = true;
            raise(new BudgetThresholdReached(userId, period, THRESHOLD_EXCEEDED_PERCENT,
                currentSpend, amount, updatedAt));
        }
    }

    private java.math.BigDecimal percentOf(int percent) {
        return amount.amount()
            .multiply(java.math.BigDecimal.valueOf(percent))
            .movePointLeft(2);
    }

    private static Money validateAmount(Money amount) {
        Objects.requireNonNull(amount);
        if (amount.isNegative()) {
            throw new BusinessRuleViolation("budget.amount_negative",
                "Budget amount cannot be negative");
        }
        return amount;
    }

    private void raise(Object event) {
        domainEvents.add(event);
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public UUID id() { return id; }
    public UserId userId() { return userId; }
    public YearMonth period() { return period; }
    public Money amount() { return amount; }
    public Money currentSpend() { return currentSpend; }
    public boolean warningSent() { return warningSent; }
    public boolean exceededSent() { return exceededSent; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
