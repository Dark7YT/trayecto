package com.trayecto.iam.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 10, max = 72) String newPassword
) {}
