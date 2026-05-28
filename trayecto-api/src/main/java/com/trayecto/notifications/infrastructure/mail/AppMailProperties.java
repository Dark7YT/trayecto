package com.trayecto.notifications.infrastructure.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Configuración del módulo de mail.
 *
 * <ul>
 *   <li>{@code from} — remitente (ej. "Trayecto &lt;noreply@trayecto.app&gt;").
 *       Para Brevo: debe ser el email verificado como sender en su dashboard.</li>
 *   <li>{@code appUrl} — URL pública del frontend para armar enlaces de email</li>
 *   <li>{@code provider} — "smtp" (default, dev local con Gmail) | "resend" (sandbox, solo a TU email) | "brevo" (prod abierta, requiere sender verification)</li>
 *   <li>{@code resendApiKey} — obligatorio si provider="resend", env var {@code RESEND_API_KEY}</li>
 *   <li>{@code brevoApiKey} — obligatorio si provider="brevo", env var {@code BREVO_API_KEY} (formato xkeysib-...)</li>
 * </ul>
 */
@Validated
@ConfigurationProperties("app.mail")
public record AppMailProperties(
    @NotBlank String from,
    @NotBlank String appUrl,
    String provider,
    String resendApiKey,
    String brevoApiKey
) {}
