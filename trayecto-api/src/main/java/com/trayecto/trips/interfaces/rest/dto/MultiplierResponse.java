package com.trayecto.trips.interfaces.rest.dto;

import com.trayecto.trips.domain.UserMultiplier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MultiplierResponse(
    UUID id,
    String name,
    BigDecimal value,
    boolean isDefault,
    Instant createdAt,
    Instant updatedAt
) {
    public static MultiplierResponse from(UserMultiplier m) {
        return new MultiplierResponse(
            m.id().value(), m.name(), m.value(), m.isDefault(), m.createdAt(), m.updatedAt()
        );
    }
}
