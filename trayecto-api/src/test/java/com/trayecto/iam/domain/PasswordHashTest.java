package com.trayecto.iam.domain;

import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordHashTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "$2a$12$0123456789012345678901abc",
        "$2b$12$0123456789012345678901abc",
        "$2y$12$0123456789012345678901abc"
    })
    void acceptsValidBcryptFormats(String hash) {
        PasswordHash ph = new PasswordHash(hash);
        assertThat(ph.value()).isEqualTo(hash);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "plaintext-password",
        "$1$invalid",
        "md5:abcdef",
        "$2c$invalid-variant"
    })
    void rejectsNonBcryptFormats(String invalid) {
        assertThatThrownBy(() -> new PasswordHash(invalid))
            .isInstanceOf(BusinessRuleViolation.class)
            .hasMessageContaining("bcrypt");
    }

    @Test
    void rejectsBlank() {
        assertThatThrownBy(() -> new PasswordHash(""))
            .isInstanceOf(BusinessRuleViolation.class);
        assertThatThrownBy(() -> new PasswordHash("   "))
            .isInstanceOf(BusinessRuleViolation.class);
    }
}
