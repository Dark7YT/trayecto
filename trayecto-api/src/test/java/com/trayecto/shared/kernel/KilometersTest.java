package com.trayecto.shared.kernel;

import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KilometersTest {

    @Test
    void valueIsScaledToOneDecimal() {
        Kilometers k = Kilometers.of(new BigDecimal("123.456"));
        assertThat(k.value()).isEqualByComparingTo("123.5"); // HALF_EVEN
    }

    @Test
    void rejectsNegativeValues() {
        assertThatThrownBy(() -> Kilometers.of(-1))
            .isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code()).isEqualTo("kilometers.negative"));
    }

    @Test
    void rejectsAboveMaxRange() {
        assertThatThrownBy(() -> Kilometers.of(new BigDecimal("9999999999")))
            .isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code()).isEqualTo("kilometers.out_of_range"));
    }

    @Test
    void distanceToCalculatesDifference() {
        Kilometers start = Kilometers.of(100);
        Kilometers end = Kilometers.of(150.5);
        assertThat(start.distanceTo(end).value()).isEqualByComparingTo("50.5");
    }

    @Test
    void distanceToRejectsBackwards() {
        Kilometers start = Kilometers.of(200);
        Kilometers end = Kilometers.of(100);
        assertThatThrownBy(() -> start.distanceTo(end))
            .isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code()).isEqualTo("kilometers.end_before_start"));
    }
}
