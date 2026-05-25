package com.trayecto.trips.domain;

import com.trayecto.shared.kernel.Kilometers;

import java.util.Optional;

/**
 * Puerto del dominio para reconocimiento óptico de la lectura del odómetro.
 * La implementación real vive en {@code trips.infrastructure.ocr} (Google Cloud Vision).
 * En dev o sin credenciales, puede haber un adapter no-op que devuelve {@link Optional#empty()}.
 */
public interface OdometerOcrPort {

    /**
     * Intenta detectar la lectura numérica del odómetro en una imagen.
     * @return {@link Optional#empty()} si no se pudo detectar con confianza suficiente.
     */
    Optional<DetectedReading> detect(byte[] imageBytes, String contentType);

    record DetectedReading(Kilometers reading, double confidence) {}
}
