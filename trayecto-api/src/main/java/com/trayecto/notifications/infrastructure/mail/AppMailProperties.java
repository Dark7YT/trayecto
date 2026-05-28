package com.trayecto.notifications.infrastructure.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Configuración del módulo de mail.
 *
 * <ul>
 *   <li>{@code from} — remitente (ej. "Trayecto &lt;noreply@trayecto.app&gt;")</li>
 *   <li>{@code appUrl} — URL pública del frontend para armar enlaces de email</li>
 *   <li>{@code provider} — "smtp" (default, dev local con Gmail/Brevo) o "resend" (prod en Render)</li>
 *   <li>{@code resendApiKey} — obligatorio si provider="resend", se lee de la env var {@code RESEND_API_KEY}</li>
 * </ul>
 */
@Validated
@ConfigurationProperties("app.mail")
public record AppMailProperties(
    @NotBlank String from,
    @NotBlank String appUrl,
    String provider,
    String resendApiKey
) {}
