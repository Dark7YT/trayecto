package com.trayecto.shared.kernel;

import com.trayecto.shared.kernel.exception.BusinessRuleViolation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Lectura del odómetro o distancia recorrida. Valor en kilómetros con un decimal.
 * Se persiste como NUMERIC(8,1) en BD (hasta 9 999 999.9 km, suficiente para la vida útil
 * de cualquier vehículo).
 */
public record Kilometers(BigDecimal value) {

    private static final BigDecimal MIN = BigDecimal.ZERO;
    private static final BigDecimal MAX = new BigDecimal("9999999.9");
    private static final int SCALE = 1;

    public Kilometers {
        Objects.requireNonNull(value, "Kilometers.value must not be null");
        value = value.setScale(SCALE, RoundingMode.HALF_EVEN);
        if (value.compareTo(MIN) < 0) {
            throw new BusinessRuleViolation("kilometers.negative",
                "Kilometers cannot be negative");
        }
        if (value.compareTo(MAX) > 0) {
            throw new BusinessRuleViolation("kilometers.out_of_range",
                "Kilometers exceeds maximum (" + MAX + ")");
        }
    }

    public static Kilometers of(BigDecimal value) {
        return new Kilometers(value);
    }

    public static Kilometers of(double value) {
        return new Kilometers(BigDecimal.valueOf(value));
    }

    public static Kilometers of(long value) {
        return new Kilometers(BigDecimal.valueOf(value));
    }

    /** Distancia = endKm - startKm. Lanza si el resultado es negativo (no se retrocede). */
    public Kilometers distanceTo(Kilometers endReading) {
        Objects.requireNonNull(endReading);
        if (endReading.value.compareTo(this.value) < 0) {
            throw new BusinessRuleViolation("kilometers.end_before_start",
                "End reading must be greater than or equal to start reading");
        }
        return new Kilometers(endReading.value.subtract(this.value));
    }

    public boolean isGreaterThan(Kilometers other) {
        return value.compareTo(other.value) > 0;
    }

    public boolean isZero() {
        return value.signum() == 0;
    }
}
