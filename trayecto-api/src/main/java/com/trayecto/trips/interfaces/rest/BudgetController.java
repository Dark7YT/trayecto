package com.trayecto.trips.interfaces.rest;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.NotFoundException;
import com.trayecto.trips.application.command.SetMonthlyBudgetHandler;
import com.trayecto.trips.application.query.GetMonthlyBudgetHandler;
import com.trayecto.trips.interfaces.rest.dto.BudgetResponse;
import com.trayecto.trips.interfaces.rest.dto.SetBudgetRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping(value = "/api/v1/budget", produces = MediaType.APPLICATION_JSON_VALUE)
class BudgetController {

    private final SetMonthlyBudgetHandler setHandler;
    private final GetMonthlyBudgetHandler getHandler;

    BudgetController(SetMonthlyBudgetHandler setHandler, GetMonthlyBudgetHandler getHandler) {
        this.setHandler = setHandler;
        this.getHandler = getHandler;
    }

    @GetMapping("/{year}/{month}")
    ResponseEntity<BudgetResponse> get(
        @AuthenticationPrincipal UserId userId,
        @PathVariable int year,
        @PathVariable int month
    ) {
        requireAuth(userId);
        YearMonth period = YearMonth.of(year, month);
        var budget = getHandler.handle(new GetMonthlyBudgetHandler.Query(userId, period))
            .orElseThrow(() -> new NotFoundException("budget.not_found",
                "No budget set for " + period));
        return ResponseEntity.ok(BudgetResponse.from(budget));
    }

    @PutMapping("/{year}/{month}")
    ResponseEntity<BudgetResponse> set(
        @AuthenticationPrincipal UserId userId,
        @PathVariable int year,
        @PathVariable int month,
        @Valid @RequestBody SetBudgetRequest req
    ) {
        requireAuth(userId);
        YearMonth period = YearMonth.of(year, month);
        setHandler.handle(new SetMonthlyBudgetHandler.Command(userId, period, req.amount()));
        var budget = getHandler.handle(new GetMonthlyBudgetHandler.Query(userId, period)).orElseThrow();
        return ResponseEntity.ok(BudgetResponse.from(budget));
    }

    private static void requireAuth(UserId userId) {
        if (userId == null) throw new AccessDeniedException("Authentication required");
    }
}
