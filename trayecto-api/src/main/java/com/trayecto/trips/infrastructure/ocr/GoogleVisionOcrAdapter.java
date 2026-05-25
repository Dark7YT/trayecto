package com.trayecto.trips.infrastructure.ocr;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.protobuf.ByteString;
import com.trayecto.shared.kernel.Kilometers;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.trips.domain.OdometerOcrPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Google Cloud Vision OCR del odómetro. Usa {@code DOCUMENT_TEXT_DETECTION} y extrae el
 * primer número plausible (4-7 dígitos enteros, opcional decimal con un dígito).
 * <p>
 * Credenciales: JSON de service account codificado en base64 en {@code app.ocr.google-credentials-base64}.
 * Si no está, el adapter loguea WARN y todas las detecciones devuelven {@link Optional#empty()}.
 */
@Service
public class GoogleVisionOcrAdapter implements OdometerOcrPort {

    private static final Logger log = LoggerFactory.getLogger(GoogleVisionOcrAdapter.class);
    private static final Pattern KM_PATTERN = Pattern.compile("(\\d{4,7})(?:[.,](\\d))?");

    private final OcrProperties props;
    private final ImageAnnotatorSettings settings; // null si OCR disabled

    public GoogleVisionOcrAdapter(OcrProperties props) {
        this.props = props;
        ImageAnnotatorSettings built = null;
        if (props.isEnabled()) {
            try {
                byte[] credentialsJson = Base64.getDecoder().decode(props.googleCredentialsBase64());
                GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(credentialsJson)
                );
                built = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();
                log.info("Google Vision OCR enabled (monthlyQuotaPerUser={})", props.monthlyQuotaPerUser());
            } catch (Exception e) {
                log.warn("Failed to initialize Google Vision credentials, OCR disabled: {}", e.getMessage());
            }
        } else {
            log.warn("Google Vision OCR disabled — no credentials configured");
        }
        this.settings = built;
    }

    @Override
    public Optional<DetectedReading> detect(byte[] imageBytes, String contentType) {
        if (settings == null) return Optional.empty();
        if (imageBytes == null || imageBytes.length == 0) {
            throw new BusinessRuleViolation("ocr.empty_image", "Image bytes are empty");
        }

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
            Image image = Image.newBuilder().setContent(ByteString.copyFrom(imageBytes)).build();
            Feature feature = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feature).setImage(image).build();

            BatchAnnotateImagesResponse responses = client.batchAnnotateImages(List.of(request));
            if (responses.getResponsesCount() == 0) return Optional.empty();

            AnnotateImageResponse response = responses.getResponses(0);
            if (response.hasError()) {
                log.warn("Vision API error: {}", response.getError().getMessage());
                return Optional.empty();
            }
            String fullText = response.getFullTextAnnotation().getText();
            if (fullText == null || fullText.isBlank()) return Optional.empty();

            return extractReading(fullText);
        } catch (Exception e) {
            log.warn("OCR call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<DetectedReading> extractReading(String text) {
        Matcher matcher = KM_PATTERN.matcher(text);
        BigDecimal best = null;
        while (matcher.find()) {
            String intPart = matcher.group(1);
            String fracPart = matcher.group(2);
            String numeric = fracPart != null ? intPart + "." + fracPart : intPart;
            BigDecimal candidate = new BigDecimal(numeric);
            // Heurística: el odómetro suele estar entre 100 y 999 999. Elegimos el primer match.
            if (candidate.compareTo(new BigDecimal("100")) >= 0
                && candidate.compareTo(new BigDecimal("9999999.9")) <= 0) {
                best = candidate;
                break;
            }
        }
        if (best == null) return Optional.empty();
        // Confianza basada en si el texto era simple (un número claro) o complejo.
        double confidence = text.trim().length() < 30 ? 0.95 : 0.75;
        if (confidence < props.minConfidence()) return Optional.empty();
        return Optional.of(new DetectedReading(Kilometers.of(best), confidence));
    }
}
