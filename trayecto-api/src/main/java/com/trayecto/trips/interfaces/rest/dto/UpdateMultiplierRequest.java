package com.trayecto.trips.interfaces.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateMultiplierRequest(
    @NotBlank @Size(max = 50) String name,
    @NotNull @DecimalMin("0.01") @DecimalMax("99.99") BigDecimal value
) {}
