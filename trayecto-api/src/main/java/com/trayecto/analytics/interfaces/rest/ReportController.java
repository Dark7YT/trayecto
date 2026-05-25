package com.trayecto.analytics.interfaces.rest;

import com.trayecto.analytics.application.GenerateCustomReportHandler;
import com.trayecto.analytics.application.GenerateMonthlyReportHandler;
import com.trayecto.analytics.interfaces.rest.dto.CustomReportRequest;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.sharing.api.SharingPublicApi;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Endpoints de reporte:
 *
 *  <ul>
 *    <li>{@code GET /monthly/{year}/{month}.{format}} — reporte mensual del propio usuario.</li>
 *    <li>{@code POST /custom.{format}} — reporte de selección arbitraria de viajes
 *        (body: {@code {tripIds: [...]}}). Si la lista es vacía → todos los viajes
 *        COMPLETED del usuario.</li>
 *    <li>{@code GET /shared/{ownerId}/monthly/{year}/{month}.{format}} — reporte
 *        mensual del owner, requiere access grant ACCEPTED.</li>
 *    <li>{@code POST /shared/{ownerId}/custom.{format}} — selección arbitraria de
 *        los viajes del owner, requiere access grant ACCEPTED.</li>
 *  </ul>
 *
 * Estrategia POST + body para el custom: el cliente puede enviar decenas o cientos
 * de tripIds sin chocar con el límite de URL (~2KB).
 */
@RestController
@RequestMapping("/api/v1/reports")
class ReportController {

    private static final MediaType XLSX_MEDIA_TYPE =
        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final GenerateMonthlyReportHandler monthlyHandler;
    private final GenerateCustomReportHandler customHandler;
    private final SharingPublicApi sharingApi;

    ReportController(
        GenerateMonthlyReportHandler monthlyHandler,
        GenerateCustomReportHandler customHandler,
        SharingPublicApi sharingApi
    ) {
        this.monthlyHandler = monthlyHandler;
        this.customHandler = customHandler;
        this.sharingApi = sharingApi;
    }

    // ───────── Reporte mensual propio ─────────

    @GetMapping("/monthly/{year}/{month}.pdf")
    ResponseEntity<byte[]> monthlyPdf(
        @AuthenticationPrincipal UserId userId,
        @PathVariable int year,
        @PathVariable int month
    ) {
        requireAuth(userId);
        byte[] body = monthlyHandler.handle(new GenerateMonthlyReportHandler.Command(
            userId, year, month, GenerateMonthlyReportHandler.Format.PDF));
        return pdfResponse(body, monthlyFilename(year, month, "pdf"));
    }

    @GetMapping("/monthly/{year}/{month}.xlsx")
    ResponseEntity<byte[]> monthlyXlsx(
        @AuthenticationPrincipal UserId userId,
        @PathVariable int year,
        @PathVariable int month
    ) {
        requireAuth(userId);
        byte[] body = monthlyHandler.handle(new GenerateMonthlyReportHandler.Command(
            userId, year, month, GenerateMonthlyReportHandler.Format.XLSX));
        return xlsxResponse(body, monthlyFilename(year, month, "xlsx"));
    }

    // ───────── Reporte custom propio (selección) ─────────

    @PostMapping("/custom.pdf")
    ResponseEntity<byte[]> customPdf(
        @AuthenticationPrincipal UserId userId,
        @RequestBody(required = false) CustomReportRequest req
    ) {
        requireAuth(userId);
        byte[] body = customHandler.handle(new GenerateCustomReportHandler.Command(
            userId, asSet(req), GenerateMonthlyReportHandler.Format.PDF));
        return pdfResponse(body, customFilename("pdf"));
    }

    @PostMapping("/custom.xlsx")
    ResponseEntity<byte[]> customXlsx(
        @AuthenticationPrincipal UserId userId,
        @RequestBody(required = false) CustomReportRequest req
    ) {
        requireAuth(userId);
        byte[] body = customHandler.handle(new GenerateCustomReportHandler.Command(
            userId, asSet(req), GenerateMonthlyReportHandler.Format.XLSX));
        return xlsxResponse(body, customFilename("xlsx"));
    }

    // ───────── Reporte mensual del owner (grantee) ─────────

    @GetMapping("/shared/{ownerId}/monthly/{year}/{month}.pdf")
    ResponseEntity<byte[]> sharedMonthlyPdf(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID ownerId,
        @PathVariable int year,
        @PathVariable int month
    ) {
        requireAuth(userId);
        UserId owner = UserId.of(ownerId);
        requireSharingAccess(userId, owner);
        byte[] body = monthlyHandler.handle(new GenerateMonthlyReportHandler.Command(
            owner, year, month, GenerateMonthlyReportHandler.Format.PDF));
        return pdfResponse(body, monthlyFilename(year, month, "pdf"));
    }

    @GetMapping("/shared/{ownerId}/monthly/{year}/{month}.xlsx")
    ResponseEntity<byte[]> sharedMonthlyXlsx(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID ownerId,
        @PathVariable int year,
        @PathVariable int month
    ) {
        requireAuth(userId);
        UserId owner = UserId.of(ownerId);
        requireSharingAccess(userId, owner);
        byte[] body = monthlyHandler.handle(new GenerateMonthlyReportHandler.Command(
            owner, year, month, GenerateMonthlyReportHandler.Format.XLSX));
        return xlsxResponse(body, monthlyFilename(year, month, "xlsx"));
    }

    // ───────── Reporte custom del owner (grantee selecciona) ─────────

    @PostMapping("/shared/{ownerId}/custom.pdf")
    ResponseEntity<byte[]> sharedCustomPdf(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID ownerId,
        @RequestBody(required = false) CustomReportRequest req
    ) {
        requireAuth(userId);
        UserId owner = UserId.of(ownerId);
        requireSharingAccess(userId, owner);
        byte[] body = customHandler.handle(new GenerateCustomReportHandler.Command(
            owner, asSet(req), GenerateMonthlyReportHandler.Format.PDF));
        return pdfResponse(body, customFilename("pdf"));
    }

    @PostMapping("/shared/{ownerId}/custom.xlsx")
    ResponseEntity<byte[]> sharedCustomXlsx(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID ownerId,
        @RequestBody(required = false) CustomReportRequest req
    ) {
        requireAuth(userId);
        UserId owner = UserId.of(ownerId);
        requireSharingAccess(userId, owner);
        byte[] body = customHandler.handle(new GenerateCustomReportHandler.Command(
            owner, asSet(req), GenerateMonthlyReportHandler.Format.XLSX));
        return xlsxResponse(body, customFilename("xlsx"));
    }

    // ───────── Helpers ─────────

    private static Set<UUID> asSet(CustomReportRequest req) {
        if (req == null || req.tripIds() == null) return null;
        return new HashSet<>(req.tripIds());
    }

    private static String monthlyFilename(int year, int month, String ext) {
        return String.format("trayecto-%04d-%02d.%s", year, month, ext);
    }

    private static String customFilename(String ext) {
        long ts = System.currentTimeMillis();
        return String.format("trayecto-export-%d.%s", ts, ext);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] body, String filename) {
        return ResponseEntity.ok()
            .headers(attachment(filename))
            .contentType(MediaType.APPLICATION_PDF)
            .body(body);
    }

    private ResponseEntity<byte[]> xlsxResponse(byte[] body, String filename) {
        return ResponseEntity.ok()
            .headers(attachment(filename))
            .contentType(XLSX_MEDIA_TYPE)
            .body(body);
    }

    private static HttpHeaders attachment(String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return headers;
    }

    private void requireSharingAccess(UserId viewer, UserId owner) {
        if (!sharingApi.canViewTripsOf(viewer, owner)) {
            throw new AccessDeniedException("No tienes acceso a los viajes de este usuario");
        }
    }

    private static void requireAuth(UserId userId) {
        if (userId == null) throw new AccessDeniedException("Authentication required");
    }
}
