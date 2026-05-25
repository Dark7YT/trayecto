package com.trayecto.trips.interfaces.rest;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.trips.application.command.CancelTripHandler;
import com.trayecto.trips.application.command.CompleteTripHandler;
import com.trayecto.trips.application.command.DeleteTripHandler;
import com.trayecto.trips.application.command.EditTripHandler;
import com.trayecto.trips.application.command.StartTripHandler;
import com.trayecto.trips.application.query.GetTripHandler;
import com.trayecto.trips.application.query.ListTripsHandler;
import com.trayecto.trips.domain.MultiplierId;
import com.trayecto.trips.domain.PhotoStoragePort;
import com.trayecto.shared.kernel.TripId;
import com.trayecto.trips.domain.TripStatus;
import com.trayecto.trips.interfaces.rest.dto.CompleteTripRequest;
import com.trayecto.trips.interfaces.rest.dto.EditTripRequest;
import com.trayecto.trips.interfaces.rest.dto.OcrResponse;
import com.trayecto.trips.interfaces.rest.dto.StartTripRequest;
import com.trayecto.trips.interfaces.rest.dto.TripResponse;
import com.trayecto.trips.infrastructure.ocr.OcrProperties;
import com.trayecto.trips.domain.OdometerOcrPort;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/trips", produces = MediaType.APPLICATION_JSON_VALUE)
class TripController {

    private final StartTripHandler startTrip;
    private final CompleteTripHandler completeTrip;
    private final CancelTripHandler cancelTrip;
    private final EditTripHandler editTrip;
    private final DeleteTripHandler deleteTrip;
    private final GetTripHandler getTrip;
    private final ListTripsHandler listTrips;
    private final PhotoStoragePort photoStorage;
    private final OdometerOcrPort ocr;
    private final OcrProperties ocrProps;

    TripController(
        StartTripHandler startTrip,
        CompleteTripHandler completeTrip,
        CancelTripHandler cancelTrip,
        EditTripHandler editTrip,
        DeleteTripHandler deleteTrip,
        GetTripHandler getTrip,
        ListTripsHandler listTrips,
        PhotoStoragePort photoStorage,
        OdometerOcrPort ocr,
        OcrProperties ocrProps
    ) {
        this.startTrip = startTrip;
        this.completeTrip = completeTrip;
        this.cancelTrip = cancelTrip;
        this.editTrip = editTrip;
        this.deleteTrip = deleteTrip;
        this.getTrip = getTrip;
        this.listTrips = listTrips;
        this.photoStorage = photoStorage;
        this.ocr = ocr;
        this.ocrProps = ocrProps;
    }

    @GetMapping
    ResponseEntity<List<TripResponse>> list(
        @AuthenticationPrincipal UserId userId,
        @RequestParam(required = false) TripStatus status,
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to
    ) {
        requireAuth(userId);
        var trips = listTrips.handle(new ListTripsHandler.Query(userId, status, from, to))
            .stream().map(TripResponse::from).toList();
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/{id}")
    ResponseEntity<TripResponse> get(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID id
    ) {
        requireAuth(userId);
        var trip = getTrip.handle(new GetTripHandler.Query(userId, TripId.of(id)));
        return ResponseEntity.ok(TripResponse.from(trip));
    }

    @PostMapping
    ResponseEntity<Map<String, Object>> start(
        @AuthenticationPrincipal UserId userId,
        @Valid @RequestBody StartTripRequest req
    ) {
        requireAuth(userId);
        var result = startTrip.handle(new StartTripHandler.Command(
            userId, req.name(), req.startKm(), req.startPhotoUrl()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "tripId", result.tripId().value(),
            "status", "PENDING"
        ));
    }

    @PatchMapping("/{id}/complete")
    ResponseEntity<TripResponse> complete(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID id,
        @Valid @RequestBody CompleteTripRequest req
    ) {
        requireAuth(userId);
        completeTrip.handle(new CompleteTripHandler.Command(
            userId, TripId.of(id), req.endKm(),
            MultiplierId.of(req.multiplierId()), req.endPhotoUrl()
        ));
        var trip = getTrip.handle(new GetTripHandler.Query(userId, TripId.of(id)));
        return ResponseEntity.ok(TripResponse.from(trip));
    }

    @PatchMapping("/{id}/cancel")
    ResponseEntity<Void> cancel(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID id
    ) {
        requireAuth(userId);
        cancelTrip.handle(new CancelTripHandler.Command(userId, TripId.of(id)));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    ResponseEntity<TripResponse> edit(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID id,
        @Valid @RequestBody EditTripRequest req
    ) {
        requireAuth(userId);
        editTrip.handle(new EditTripHandler.Command(
            userId, TripId.of(id), req.name(), req.startKm(), req.endKm()
        ));
        var trip = getTrip.handle(new GetTripHandler.Query(userId, TripId.of(id)));
        return ResponseEntity.ok(TripResponse.from(trip));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID id
    ) {
        requireAuth(userId);
        deleteTrip.handle(new DeleteTripHandler.Command(userId, TripId.of(id)));
        return ResponseEntity.noContent().build();
    }

    /**
     * Sube una foto del odómetro y opcionalmente intenta detectar el km con OCR.
     * Si Cloudinary o Vision no están configurados, devuelve {@code null} en los campos
     * correspondientes — el frontend debe ofrecer entrada manual como fallback.
     */
    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<OcrResponse> ocr(
        @AuthenticationPrincipal UserId userId,
        @RequestPart("image") MultipartFile image
    ) throws IOException {
        requireAuth(userId);
        if (image == null || image.isEmpty()) {
            throw new BusinessRuleViolation("ocr.image_required", "Image file is required");
        }
        if (image.getSize() > 5 * 1024 * 1024) {
            throw new BusinessRuleViolation("ocr.image_too_large", "Image must be at most 5MB");
        }

        byte[] bytes = image.getBytes();
        String contentType = image.getContentType() != null ? image.getContentType() : "image/jpeg";

        // Storage (puede fallar si está deshabilitado)
        String photoUrl = null;
        try {
            photoUrl = photoStorage.upload(bytes, contentType, userId).secureUrl();
        } catch (BusinessRuleViolation e) {
            if (!"storage.disabled".equals(e.code())) throw e;
        }

        // OCR (puede devolver empty)
        BigDecimal detectedKm = null;
        Double confidence = null;
        Optional<OdometerOcrPort.DetectedReading> reading = ocr.detect(bytes, contentType);
        if (reading.isPresent()) {
            detectedKm = reading.get().reading().value();
            confidence = reading.get().confidence();
        }

        return ResponseEntity.ok(new OcrResponse(photoUrl, detectedKm, confidence));
    }

    private static void requireAuth(UserId userId) {
        if (userId == null) throw new AccessDeniedException("Authentication required");
    }
}
