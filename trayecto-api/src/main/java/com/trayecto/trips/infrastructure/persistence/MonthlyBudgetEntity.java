package com.trayecto.trips.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "trips_monthly_budgets",
    indexes = @Index(name = "ix_trips_budget_user", columnList = "user_id"),
    uniqueConstraints = @UniqueConstraint(
        name = "ux_trips_budget_user_period",
        columnNames = {"user_id", "year", "month"}
    )
)
class MonthlyBudgetEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "year", nullable = false)
    int year;

    @Column(name = "month", nullable = false)
    int month;

    @Column(name = "amount_value", nullable = false, precision = 12, scale = 2)
    BigDecimal amountValue;

    @Column(name = "amount_currency", nullable = false, length = 3)
    String amountCurrency;

    @Column(name = "current_spend_value", nullable = false, precision = 12, scale = 2)
    BigDecimal currentSpendValue;

    @Column(name = "current_spend_currency", nullable = false, length = 3)
    String currentSpendCurrency;

    @Column(name = "warning_sent", nullable = false)
    boolean warningSent;

    @Column(name = "exceeded_sent", nullable = false)
    boolean exceededSent;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    MonthlyBudgetEntity() {}
}
