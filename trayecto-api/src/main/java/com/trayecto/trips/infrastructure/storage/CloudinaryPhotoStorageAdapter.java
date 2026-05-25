package com.trayecto.trips.infrastructure.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.trips.domain.PhotoStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Cloudinary 2.x. Sube las fotos del odómetro al folder {@code trayecto/odometer/{userId}}.
 * Transformación: auto-format + auto-quality para optimizar bandwidth.
 * <p>
 * Si las credenciales están vacías ({@link CloudinaryProperties#isEnabled()} = false),
 * cualquier llamada lanza {@code BusinessRuleViolation("storage.disabled")} con mensaje
 * claro. La app sigue funcionando — solo no se pueden subir fotos.
 */
@Service
public class CloudinaryPhotoStorageAdapter implements PhotoStoragePort {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryPhotoStorageAdapter.class);

    private final CloudinaryProperties props;
    private final Cloudinary client;

    public CloudinaryPhotoStorageAdapter(CloudinaryProperties props) {
        this.props = props;
        if (props.isEnabled()) {
            this.client = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", props.cloudName(),
                "api_key", props.apiKey(),
                "api_secret", props.apiSecret(),
                "secure", true
            ));
            log.info("Cloudinary enabled (cloud_name={})", props.cloudName());
        } else {
            this.client = null;
            log.warn("Cloudinary disabled — no credentials configured");
        }
    }

    @Override
    public StoredPhoto upload(byte[] imageBytes, String contentType, UserId userId) {
        requireEnabled();
        Map<String, Object> params = new HashMap<>();
        params.put("folder", "trayecto/odometer/" + userId.asString());
        params.put("resource_type", "image");
        params.put("fetch_format", "auto");
        params.put("quality", "auto");
        if (props.uploadPreset() != null && !props.uploadPreset().isBlank()) {
            params.put("upload_preset", props.uploadPreset());
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = client.uploader().upload(imageBytes, params);
            String publicId = (String) result.get("public_id");
            String secureUrl = (String) result.get("secure_url");
            if (publicId == null || secureUrl == null) {
                throw new BusinessRuleViolation("storage.invalid_response",
                    "Cloudinary returned an unexpected payload");
            }
            return new StoredPhoto(publicId, secureUrl);
        } catch (IOException e) {
            log.warn("Cloudinary upload failed: {}", e.getMessage());
            throw new BusinessRuleViolation("storage.upload_failed",
                "Could not upload photo: " + e.getMessage());
        }
    }

    @Override
    public void delete(String publicId) {
        requireEnabled();
        try {
            client.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.warn("Cloudinary delete failed for {}: {}", publicId, e.getMessage());
            // No re-lanzamos: la foto huérfana se limpia con job programado, no rompemos el flujo.
        }
    }

    private void requireEnabled() {
        if (!props.isEnabled() || client == null) {
            throw new BusinessRuleViolation("storage.disabled",
                "Photo storage is not configured (set CLOUDINARY_* env vars)");
        }
    }
}
