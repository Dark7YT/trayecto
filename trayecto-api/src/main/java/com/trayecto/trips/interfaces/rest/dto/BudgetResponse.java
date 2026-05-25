package com.trayecto.trips.interfaces.rest.dto;

import com.trayecto.trips.domain.MonthlyBudget;

import java.math.BigDecimal;

public record BudgetResponse(
    int year,
    int month,
    BigDecimal amount,
    BigDecimal currentSpend,
    String currency,
    double percentUsed
) {
    public static BudgetResponse from(MonthlyBudget budget) {
        BigDecimal amount = budget.amount().amount();
        BigDecimal spend = budget.currentSpend().amount();
        double pct = amount.signum() == 0 ? 0.0
            : spend.multiply(BigDecimal.valueOf(100)).divide(amount, 2, java.math.RoundingMode.HALF_EVEN).doubleValue();
        return new BudgetResponse(
            budget.period().getYear(),
            budget.period().getMonthValue(),
            amount,
            spend,
            budget.amount().currency().getCurrencyCode(),
            pct
        );
    }
}
