package com.trayecto.shared.kernel;

import com.trayecto.shared.kernel.exception.BusinessRuleViolation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Valor monetario inmutable. Default currency = PEN (soles peruanos).
 * Toda operación aritmética retorna una nueva instancia.
 */
public record Money(BigDecimal amount, Currency currency) {

    public static final Currency PEN = Currency.getInstance("PEN");

    public Money {
        Objects.requireNonNull(amount, "Money.amount must not be null");
        Objects.requireNonNull(currency, "Money.currency must not be null");
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
    }

    public static Money pen(BigDecimal amount) {
        return new Money(amount, PEN);
    }

    public static Money pen(double amount) {
        return pen(BigDecimal.valueOf(amount));
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money zeroPen() {
        return zero(PEN);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money multipliedBy(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor must not be null");
        return new Money(amount.multiply(factor), currency);
    }

    public Money multipliedBy(double factor) {
        return multipliedBy(BigDecimal.valueOf(factor));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!currency.equals(other.currency)) {
            throw new BusinessRuleViolation("money.currency_mismatch",
                "Cannot operate on Money with different currencies: "
                    + currency.getCurrencyCode() + " vs " + other.currency.getCurrencyCode());
        }
    }
}
