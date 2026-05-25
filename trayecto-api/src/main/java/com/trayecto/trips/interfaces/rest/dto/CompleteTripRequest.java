package com.trayecto.trips.interfaces.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CompleteTripRequest(
    @NotNull @DecimalMin("0.0") @DecimalMax("9999999.9") BigDecimal endKm,
    @NotNull UUID multiplierId,
    @Size(max = 512) String endPhotoUrl
) {}
