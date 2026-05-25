package com.trayecto.shared.kernel;

import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void normalizesToLowercaseAndTrims() {
        Email email = Email.of("  USER@Example.COM  ");
        assertThat(email.value()).isEqualTo("user@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "user@example.com",
        "user+tag@example.co.uk",
        "first.last@sub.example.org",
        "u@a.bc"
    })
    void acceptsValidEmails(String input) {
        Email email = Email.of(input);
        assertThat(email.value()).isEqualTo(input.toLowerCase().trim());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "   ",
        "missing-at.example.com",
        "@no-local.com",
        "no-domain@",
        "two@@signs.com",
        "spaces in@email.com"
    })
    void rejectsInvalidEmails(String input) {
        assertThatThrownBy(() -> Email.of(input))
            .isInstanceOf(BusinessRuleViolation.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> Email.of(null))
            .isInstanceOf(BusinessRuleViolation.class)
            .hasMessageContaining("required");
    }
}
