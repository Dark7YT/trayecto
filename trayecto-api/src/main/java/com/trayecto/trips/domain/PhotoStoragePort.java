package com.trayecto.trips.domain;

import com.trayecto.shared.kernel.UserId;

/**
 * Puerto del dominio para almacenamiento de fotos del odómetro.
 * Implementación real: Cloudinary en {@code trips.infrastructure.storage}.
 */
public interface PhotoStoragePort {

    /**
     * Sube una imagen al storage. La foto se organiza por usuario para auditoría y para
     * cumplir GDPR-like (al desactivar cuenta se pueden borrar todas).
     */
    StoredPhoto upload(byte[] imageBytes, String contentType, UserId userId);

    /** Borra una foto previamente subida (al eliminar viaje con hard-delete). */
    void delete(String publicId);

    record StoredPhoto(String publicId, String secureUrl) {}
}
