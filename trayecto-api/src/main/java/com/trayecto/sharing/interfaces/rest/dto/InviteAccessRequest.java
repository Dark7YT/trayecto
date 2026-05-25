package com.trayecto.sharing.interfaces.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InviteAccessRequest(
    @NotBlank @Email @Size(max = 254) String granteeEmail,
    String recaptchaToken
) {}
