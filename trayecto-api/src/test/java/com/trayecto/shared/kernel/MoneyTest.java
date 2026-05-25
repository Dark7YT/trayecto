package com.trayecto.shared.kernel;

import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void penFactoryProducesPenCurrencyWith2Decimals() {
        Money money = Money.pen(10);
        assertThat(money.currency()).isEqualTo(Money.PEN);
        assertThat(money.amount()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void plusAddsSameCurrency() {
        Money sum = Money.pen(5.50).plus(Money.pen(4.30));
        assertThat(sum.amount()).isEqualByComparingTo("9.80");
    }

    @Test
    void multipliedByProducesScaledAmount() {
        Money cost = Money.pen(12.30).multipliedBy(0.7);
        assertThat(cost.amount()).isEqualByComparingTo("8.61");
    }

    @Test
    void mixedCurrenciesAreRejected() {
        Money pen = Money.pen(10);
        Money usd = new Money(BigDecimal.TEN, Currency.getInstance("USD"));
        assertThatThrownBy(() -> pen.plus(usd))
            .isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code())
                .isEqualTo("money.currency_mismatch"));
    }

    @Test
    void zeroIsRecognized() {
        assertThat(Money.zeroPen().isZero()).isTrue();
        assertThat(Money.pen(0.001).isZero()).isTrue(); // rounded to 0.00
    }
}
