package com.trayecto.iam.interfaces.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 10, max = 72) String password,
    @NotBlank @Size(min = 2, max = 50) String displayName,
    @NotBlank @Size(max = 16) String locale,
    @NotBlank @Size(max = 64) String timezone,
    String recaptchaToken
) {}
