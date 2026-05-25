package com.trayecto.trips.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Vista pública de un {@code UserMultiplier}. Consumido por {@code analytics}
 * para resolver nombres legibles ("Día normal") al agrupar viajes por multiplicador.
 */
public record MultiplierSnapshot(
    UUID id,
    String name,
    BigDecimal rate,
    boolean isDefault
) {}
