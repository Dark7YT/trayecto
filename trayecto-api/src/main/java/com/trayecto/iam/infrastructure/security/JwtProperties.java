package com.trayecto.iam.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
    @NotBlank String secret,
    @Min(60) long accessTtlSeconds,
    @Min(3600) long refreshTtlSeconds
) {}
