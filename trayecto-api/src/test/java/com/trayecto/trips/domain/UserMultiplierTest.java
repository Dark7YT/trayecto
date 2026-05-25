package com.trayecto.trips.domain;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserMultiplierTest {

    private static final UserId USER = UserId.newId();

    @Test
    void create_normalizesValueToTwoDecimals() {
        UserMultiplier m = UserMultiplier.create(USER, "Día normal", new BigDecimal("0.700"), true);
        assertThat(m.value()).isEqualByComparingTo("0.70");
        assertThat(m.isDefault()).isTrue();
    }

    @Test
    void create_rejectsZeroOrNegativeValue() {
        assertThatThrownBy(() -> UserMultiplier.create(USER, "X", BigDecimal.ZERO, false))
            .isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code()).isEqualTo("multiplier.value_too_low"));
        assertThatThrownBy(() -> UserMultiplier.create(USER, "X", new BigDecimal("-0.5"), false))
            .isInstanceOf(BusinessRuleViolation.class);
    }

    @Test
    void create_rejectsValueAboveMax() {
        assertThatThrownBy(() -> UserMultiplier.create(USER, "X", new BigDecimal("100"), false))
            .isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code()).isEqualTo("multiplier.value_too_high"));
    }

    @Test
    void update_changesNameAndValue() {
        UserMultiplier m = UserMultiplier.create(USER, "Original", new BigDecimal("0.50"), false);
        m.update("Renombrado", new BigDecimal("0.80"));
        assertThat(m.name()).isEqualTo("Renombrado");
        assertThat(m.value()).isEqualByComparingTo("0.80");
    }

    @Test
    void promoteAndDemoteDefault() {
        UserMultiplier m = UserMultiplier.create(USER, "X", new BigDecimal("0.70"), false);
        assertThat(m.isDefault()).isFalse();
        m.promoteAsDefault();
        assertThat(m.isDefault()).isTrue();
        m.promoteAsDefault(); // idempotente
        assertThat(m.isDefault()).isTrue();
        m.demoteFromDefault();
        assertThat(m.isDefault()).isFalse();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> UserMultiplier.create(USER, "  ", new BigDecimal("0.7"), false))
            .isInstanceOf(BusinessRuleViolation.class);
    }
}
