package com.trayecto.trips.infrastructure.ocr;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ocr")
public record OcrProperties(
    /** JSON de credenciales de service account, codificado en base64. Vacío = OCR desactivado. */
    String googleCredentialsBase64,
    /** Límite mensual por usuario para evitar exceder el free tier de 1000/mes. */
    int monthlyQuotaPerUser,
    /** Confianza mínima (0-1) para devolver una detección. */
    double minConfidence
) {
    public boolean isEnabled() {
        return googleCredentialsBase64 != null && !googleCredentialsBase64.isBlank();
    }
}
