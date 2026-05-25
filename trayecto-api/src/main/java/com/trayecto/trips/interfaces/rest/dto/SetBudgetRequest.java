package com.trayecto.trips.interfaces.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SetBudgetRequest(
    @NotNull @DecimalMin("0.00") @DecimalMax("9999999.99") BigDecimal amount
) {}
