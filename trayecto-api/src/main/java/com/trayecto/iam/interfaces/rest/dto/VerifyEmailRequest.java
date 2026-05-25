package com.trayecto.iam.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
    @NotBlank String token
) {}
