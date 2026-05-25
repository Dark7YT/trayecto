package com.trayecto.trips.domain;

import com.trayecto.shared.kernel.Money;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.trips.api.events.BudgetThresholdReached;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonthlyBudgetTest {

    private static final UserId USER = UserId.newId();
    private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);

    @Test
    void create_startsWithZeroSpend() {
        MonthlyBudget budget = MonthlyBudget.create(USER, MAY_2026, Money.pen(500));
        assertThat(budget.currentSpend().amount()).isEqualByComparingTo("0.00");
        assertThat(budget.warningSent()).isFalse();
        assertThat(budget.exceededSent()).isFalse();
    }

    @Test
    void recordSpend_emitsWarningAt80Percent() {
        MonthlyBudget budget = MonthlyBudget.create(USER, MAY_2026, Money.pen(100));
        budget.recordSpend(Money.pen(85)); // 85% del budget

        List<Object> events = budget.pullDomainEvents();
        assertThat(events).hasSize(1);
        BudgetThresholdReached ev = (BudgetThresholdReached) events.getFirst();
        assertThat(ev.thresholdPercent()).isEqualTo(80);
        assertThat(ev.userId()).isEqualTo(USER);
    }

    @Test
    void recordSpend_emitsBothWarningAndExceededWhenCrossingTogether() {
        MonthlyBudget budget = MonthlyBudget.create(USER, MAY_2026, Money.pen(100));
        budget.recordSpend(Money.pen(120)); // 120% — cruza ambos umbrales

        List<Object> events = budget.pullDomainEvents();
        assertThat(events).hasSize(2);
        assertThat(events).extracting(e -> ((BudgetThresholdReached) e).thresholdPercent())
            .containsExactly(80, 100);
    }

    @Test
    void recordSpend_doesNotEmitAgainOnSecondCrossing() {
        MonthlyBudget budget = MonthlyBudget.create(USER, MAY_2026, Money.pen(100));
        budget.recordSpend(Money.pen(85));
        budget.pullDomainEvents();

        budget.recordSpend(Money.pen(10)); // sigue por debajo de 100%

        assertThat(budget.pullDomainEvents()).isEmpty();
    }

    @Test
    void recordSpend_rejectsNegativeAmounts() {
        MonthlyBudget budget = MonthlyBudget.create(USER, MAY_2026, Money.pen(100));
        assertThatThrownBy(() -> budget.recordSpend(Money.pen(-1)))
            .isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code()).isEqualTo("budget.spend_negative"));
    }

    @Test
    void updateAmount_resetsFlagsIfSpendBelowNewThresholds() {
        MonthlyBudget budget = MonthlyBudget.create(USER, MAY_2026, Money.pen(100));
        budget.recordSpend(Money.pen(85)); // dispara warning
        budget.pullDomainEvents();
        assertThat(budget.warningSent()).isTrue();

        // Si el usuario sube el budget a 200, el spend de 85 ya no cruza el 80% (= 160)
        budget.updateAmount(Money.pen(200));

        assertThat(budget.warningSent()).isFalse();
        // Recargar spend hasta 165 dispara warning de nuevo
        budget.recordSpend(Money.pen(80));
        assertThat(budget.warningSent()).isTrue();
    }
}
