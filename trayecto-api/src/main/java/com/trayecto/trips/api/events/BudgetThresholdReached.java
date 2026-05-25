package com.trayecto.trips.api.events;

import com.trayecto.shared.kernel.Money;
import com.trayecto.shared.kernel.UserId;

import java.time.Instant;
import java.time.YearMonth;

/**
 * Cruzó un umbral del presupuesto mensual (80% advertencia o 100% excedido).
 * {@code notifications} envía email + push.
 */
public record BudgetThresholdReached(
    UserId userId,
    YearMonth period,
    int thresholdPercent,  // 80 o 100
    Money currentSpend,
    Money budgetAmount,
    Instant reachedAt
) {}
