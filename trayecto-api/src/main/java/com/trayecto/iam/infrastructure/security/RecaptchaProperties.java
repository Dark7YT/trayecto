package com.trayecto.iam.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.recaptcha")
public record RecaptchaProperties(
    /** Secret key del backend de reCAPTCHA v3. Si está vacío, el verifier opera en modo bypass. */
    String secretKey,
    /** Umbral mínimo de score (0.0 - 1.0). 0.5 es un buen default para v3. */
    double minScore
) {
    public boolean isEnabled() {
        return secretKey != null && !secretKey.isBlank();
    }
}
