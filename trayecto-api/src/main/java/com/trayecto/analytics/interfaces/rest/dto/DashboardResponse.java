package com.trayecto.analytics.interfaces.rest.dto;

import com.trayecto.analytics.application.query.GetDashboardHandler.DashboardSnapshot;

import java.math.BigDecimal;

/**
 * DTO REST aplanado para el endpoint de dashboard. Solo primitivos JSON-friendly.
 */
public record DashboardResponse(
    BigDecimal totalKm,
    BigDecimal totalCostAmount,
    String totalCostCurrency,
    BigDecimal currentMonthCostAmount,
    BigDecimal currentMonthKm,
    int totalCompletedTrips,
    int currentMonthTrips
) {
    public static DashboardResponse from(DashboardSnapshot snapshot) {
        return new DashboardResponse(
            snapshot.totalKm(),
            snapshot.totalCostAmount(),
            snapshot.totalCostCurrency(),
            snapshot.currentMonthCostAmount(),
            snapshot.currentMonthKm(),
            snapshot.totalCompletedTrips(),
            snapshot.currentMonthTrips()
        );
    }
}
