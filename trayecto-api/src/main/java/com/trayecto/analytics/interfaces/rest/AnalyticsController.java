package com.trayecto.analytics.interfaces.rest;

import com.trayecto.analytics.application.query.GetCostByMonthHandler;
import com.trayecto.analytics.application.query.GetDashboardHandler;
import com.trayecto.analytics.application.query.GetKmByMonthHandler;
import com.trayecto.analytics.application.query.GetMultiplierUsageHandler;
import com.trayecto.analytics.interfaces.rest.dto.CostByMonthResponse;
import com.trayecto.analytics.interfaces.rest.dto.DashboardResponse;
import com.trayecto.analytics.interfaces.rest.dto.KmByMonthResponse;
import com.trayecto.analytics.interfaces.rest.dto.MultiplierUsageResponse;
import com.trayecto.shared.kernel.UserId;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Endpoints de analytics. Cada query agrega EN VIVO los viajes COMPLETED reales
 * del usuario: no hay read-models materializados ni proceso de rebuild — la
 * "fuente única de verdad" es la tabla de viajes. Esto garantiza que los KPIs
 * y gráficos siempre reflejen lo que el usuario tiene cerrado en este instante,
 * incluso tras borrar o editar viajes.
 */
@RestController
@RequestMapping(value = "/api/v1/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
class AnalyticsController {

    private final GetDashboardHandler getDashboard;
    private final GetKmByMonthHandler getKmByMonth;
    private final GetCostByMonthHandler getCostByMonth;
    private final GetMultiplierUsageHandler getMultiplierUsage;

    AnalyticsController(
        GetDashboardHandler getDashboard,
        GetKmByMonthHandler getKmByMonth,
        GetCostByMonthHandler getCostByMonth,
        GetMultiplierUsageHandler getMultiplierUsage
    ) {
        this.getDashboard = getDashboard;
        this.getKmByMonth = getKmByMonth;
        this.getCostByMonth = getCostByMonth;
        this.getMultiplierUsage = getMultiplierUsage;
    }

    @GetMapping("/dashboard")
    ResponseEntity<DashboardResponse> dashboard(@AuthenticationPrincipal UserId userId) {
        requireAuth(userId);
        var snapshot = getDashboard.handle(new GetDashboardHandler.Query(userId));
        return ResponseEntity.ok(DashboardResponse.from(snapshot));
    }

    @GetMapping("/charts/km-by-month")
    ResponseEntity<List<KmByMonthResponse>> kmByMonth(
        @AuthenticationPrincipal UserId userId,
        @RequestParam(required = false) Integer year
    ) {
        requireAuth(userId);
        int targetYear = year != null ? year : LocalDate.now(ZoneOffset.UTC).getYear();
        var points = getKmByMonth.handle(new GetKmByMonthHandler.Query(userId, targetYear));
        return ResponseEntity.ok(points.stream().map(KmByMonthResponse::from).toList());
    }

    @GetMapping("/charts/cost-by-month")
    ResponseEntity<List<CostByMonthResponse>> costByMonth(
        @AuthenticationPrincipal UserId userId,
        @RequestParam(required = false) Integer year
    ) {
        requireAuth(userId);
        int targetYear = year != null ? year : LocalDate.now(ZoneOffset.UTC).getYear();
        var points = getCostByMonth.handle(new GetCostByMonthHandler.Query(userId, targetYear));
        return ResponseEntity.ok(points.stream().map(CostByMonthResponse::from).toList());
    }

    @GetMapping("/charts/multiplier-usage")
    ResponseEntity<List<MultiplierUsageResponse>> multiplierUsage(@AuthenticationPrincipal UserId userId) {
        requireAuth(userId);
        var points = getMultiplierUsage.handle(new GetMultiplierUsageHandler.Query(userId));
        return ResponseEntity.ok(points.stream().map(MultiplierUsageResponse::from).toList());
    }

    private static void requireAuth(UserId userId) {
        if (userId == null) throw new AccessDeniedException("Authentication required");
    }
}
