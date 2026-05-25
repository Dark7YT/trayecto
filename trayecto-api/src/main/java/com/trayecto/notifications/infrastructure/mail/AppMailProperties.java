package com.trayecto.notifications.infrastructure.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties("app.mail")
public record AppMailProperties(
    @NotBlank String from,
    @NotBlank String appUrl
) {}
