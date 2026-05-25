package com.trayecto.analytics.interfaces.rest.dto;

import com.trayecto.analytics.application.query.GetCostByMonthHandler.MonthlyCostPoint;

import java.math.BigDecimal;

public record CostByMonthResponse(String yearMonth, BigDecimal amount, String currency) {
    public static CostByMonthResponse from(MonthlyCostPoint point) {
        return new CostByMonthResponse(point.yearMonth(), point.amount(), point.currency());
    }
}
