package com.trayecto.trips.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.cloudinary")
public record CloudinaryProperties(
    String cloudName,
    String apiKey,
    String apiSecret,
    String uploadPreset
) {
    public boolean isEnabled() {
        return cloudName != null && !cloudName.isBlank()
            && apiKey != null && !apiKey.isBlank()
            && apiSecret != null && !apiSecret.isBlank();
    }
}
