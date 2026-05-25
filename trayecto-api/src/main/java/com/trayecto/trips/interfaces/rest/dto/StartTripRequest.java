package com.trayecto.trips.interfaces.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record StartTripRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull @DecimalMin("0.0") @DecimalMax("9999999.9") BigDecimal startKm,
    @Size(max = 512) String startPhotoUrl
) {}
