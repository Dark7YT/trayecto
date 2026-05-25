package com.trayecto.trips.domain;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Multiplicador configurable por usuario. El value se aplica al número de km
 * recorridos para calcular el costo del viaje en soles.
 * <p>
 * Ejemplos: "Día normal" = 0.7, "Fin de semana" = 0.5, "Tráfico" = 0.9.
 * <p>
 * Las restricciones de "máximo 10 por usuario" y "solo uno isDefault" se garantizan
 * en el CommandHandler, no en el aggregate (porque requieren consultar el repo).
 */
public final class UserMultiplier {

    public static final int MAX_PER_USER = 10;
    private static final BigDecimal MIN_VALUE = new BigDecimal("0.01");
    private static final BigDecimal MAX_VALUE = new BigDecimal("99.99");
    private static final int SCALE = 2;

    private final MultiplierId id;
    private final UserId userId;
    private String name;
    private BigDecimal value;
    private boolean isDefault;
    private final Instant createdAt;
    private Instant updatedAt;

    private UserMultiplier(
        MultiplierId id, UserId userId, String name, BigDecimal value,
        boolean isDefault, Instant createdAt, Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.name = validateName(name);
        this.value = validateValue(value);
        this.isDefault = isDefault;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static UserMultiplier create(UserId userId, String name, BigDecimal value, boolean isDefault) {
        Instant now = Instant.now();
        return new UserMultiplier(MultiplierId.newId(), userId, name, value, isDefault, now, now);
    }

    public static UserMultiplier reconstitute(
        MultiplierId id, UserId userId, String name, BigDecimal value,
        boolean isDefault, Instant createdAt, Instant updatedAt
    ) {
        return new UserMultiplier(id, userId, name, value, isDefault, createdAt, updatedAt);
    }

    public void update(String newName, BigDecimal newValue) {
        this.name = validateName(newName);
        this.value = validateValue(newValue);
        this.updatedAt = Instant.now();
    }

    public void promoteAsDefault() {
        if (!isDefault) {
            isDefault = true;
            updatedAt = Instant.now();
        }
    }

    public void demoteFromDefault() {
        if (isDefault) {
            isDefault = false;
            updatedAt = Instant.now();
        }
    }

    private static String validateName(String name) {
        if (name == null) throw new BusinessRuleViolation("multiplier.name_required", "Name is required");
        name = name.trim();
        if (name.isEmpty()) throw new BusinessRuleViolation("multiplier.name_required", "Name is required");
        if (name.length() > 50) {
            throw new BusinessRuleViolation("multiplier.name_too_long", "Name must be at most 50 characters");
        }
        return name;
    }

    private static BigDecimal validateValue(BigDecimal value) {
        Objects.requireNonNull(value);
        value = value.setScale(SCALE, RoundingMode.HALF_EVEN);
        if (value.compareTo(MIN_VALUE) < 0) {
            throw new BusinessRuleViolation("multiplier.value_too_low",
                "Multiplier value must be at least " + MIN_VALUE);
        }
        if (value.compareTo(MAX_VALUE) > 0) {
            throw new BusinessRuleViolation("multiplier.value_too_high",
                "Multiplier value must be at most " + MAX_VALUE);
        }
        return value;
    }

    public MultiplierId id() { return id; }
    public UserId userId() { return userId; }
    public String name() { return name; }
    public BigDecimal value() { return value; }
    public boolean isDefault() { return isDefault; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
