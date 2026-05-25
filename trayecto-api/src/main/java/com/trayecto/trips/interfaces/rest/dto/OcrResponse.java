package com.trayecto.trips.interfaces.rest.dto;

import java.math.BigDecimal;

public record OcrResponse(
    /** URL pública de la foto subida. Null si storage no está configurado. */
    String photoUrl,
    /** Lectura detectada en km. Null si OCR no detectó o no está configurado. */
    BigDecimal detectedKm,
    /** Confianza 0-1. Null si no hubo detección. */
    Double confidence
) {}
