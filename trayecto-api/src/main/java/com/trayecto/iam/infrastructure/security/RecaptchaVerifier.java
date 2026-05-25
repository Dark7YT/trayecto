package com.trayecto.iam.infrastructure.security;

import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Valida tokens de reCAPTCHA v3 contra la API de Google.
 * <p>
 * Llamada de Google retorna:
 *   { success: true|false, score: 0.0-1.0, action: "...", challenge_ts: "...", hostname: "..." }
 * Aceptamos si {@code success && score >= minScore}.
 * <p>
 * Si el secret no está configurado (dev sin credenciales reales), el verifier es no-op:
 * todas las verificaciones pasan. En producción debe estar configurado.
 */
@Service
public class RecaptchaVerifier {

    private static final Logger log = LoggerFactory.getLogger(RecaptchaVerifier.class);
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final RecaptchaProperties props;
    private final RestClient http;

    public RecaptchaVerifier(RecaptchaProperties props) {
        this.props = props;
        this.http = RestClient.builder().baseUrl(VERIFY_URL).build();
    }

    /**
     * Verifica el token. Lanza {@link BusinessRuleViolation} con código
     * {@code auth.recaptcha_failed} si el token no es válido o el score es bajo.
     * No-op si el verifier no está habilitado.
     */
    public void verify(String token, String expectedAction) {
        if (!props.isEnabled()) {
            log.debug("reCAPTCHA verifier disabled (no secret configured) — skipping");
            return;
        }
        if (token == null || token.isBlank()) {
            throw new BusinessRuleViolation("auth.recaptcha_missing",
                "reCAPTCHA token is required");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> response = http.post()
            .uri(uriBuilder -> uriBuilder
                .queryParam("secret", props.secretKey())
                .queryParam("response", token)
                .build())
            .retrieve()
            .body(Map.class);

        if (response == null) {
            throw new BusinessRuleViolation("auth.recaptcha_failed",
                "reCAPTCHA verification returned no response");
        }
        boolean success = Boolean.TRUE.equals(response.get("success"));
        if (!success) {
            log.warn("reCAPTCHA verification failed: {}", response.get("error-codes"));
            throw new BusinessRuleViolation("auth.recaptcha_failed",
                "reCAPTCHA verification failed");
        }
        Object scoreObj = response.get("score");
        double score = scoreObj instanceof Number n ? n.doubleValue() : 0.0;
        if (score < props.minScore()) {
            log.warn("reCAPTCHA score {} below threshold {} (action={})",
                score, props.minScore(), response.get("action"));
            throw new BusinessRuleViolation("auth.recaptcha_low_score",
                "Anti-bot score is too low");
        }
        if (expectedAction != null && !expectedAction.equals(response.get("action"))) {
            log.warn("reCAPTCHA action mismatch: expected={}, got={}",
                expectedAction, response.get("action"));
            throw new BusinessRuleViolation("auth.recaptcha_action_mismatch",
                "reCAPTCHA action mismatch");
        }
    }
}
