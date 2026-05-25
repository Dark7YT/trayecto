package com.trayecto.analytics.interfaces.rest.dto;

import com.trayecto.analytics.application.query.GetMultiplierUsageHandler.MultiplierUsagePoint;

import java.math.BigDecimal;
import java.util.UUID;

public record MultiplierUsageResponse(
    UUID multiplierId,
    BigDecimal multiplierRate,
    String multiplierName,
    int count,
    BigDecimal percentage
) {
    public static MultiplierUsageResponse from(MultiplierUsagePoint point) {
        return new MultiplierUsageResponse(
            point.multiplierId(),
            point.multiplierRate(),
            point.multiplierName(),
            point.count(),
            point.percentage()
        );
    }
}
