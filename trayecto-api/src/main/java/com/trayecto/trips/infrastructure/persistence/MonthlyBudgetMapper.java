package com.trayecto.trips.infrastructure.persistence;

import com.trayecto.shared.kernel.Money;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.domain.MonthlyBudget;

import java.time.YearMonth;
import java.util.Currency;

final class MonthlyBudgetMapper {

    private MonthlyBudgetMapper() {}

    static MonthlyBudgetEntity toEntity(MonthlyBudget b, MonthlyBudgetEntity reuse) {
        MonthlyBudgetEntity e = reuse != null ? reuse : new MonthlyBudgetEntity();
        e.id = b.id();
        e.userId = b.userId().value();
        e.year = b.period().getYear();
        e.month = b.period().getMonthValue();
        e.amountValue = b.amount().amount();
        e.amountCurrency = b.amount().currency().getCurrencyCode();
        e.currentSpendValue = b.currentSpend().amount();
        e.currentSpendCurrency = b.currentSpend().currency().getCurrencyCode();
        e.warningSent = b.warningSent();
        e.exceededSent = b.exceededSent();
        e.createdAt = b.createdAt();
        e.updatedAt = b.updatedAt();
        return e;
    }

    static MonthlyBudget toDomain(MonthlyBudgetEntity e) {
        Money amount = new Money(e.amountValue, Currency.getInstance(e.amountCurrency));
        Money spend = new Money(e.currentSpendValue, Currency.getInstance(e.currentSpendCurrency));
        return MonthlyBudget.reconstitute(
            e.id,
            UserId.of(e.userId),
            YearMonth.of(e.year, e.month),
            amount,
            spend,
            e.warningSent,
            e.exceededSent,
            e.createdAt,
            e.updatedAt
        );
    }
}
