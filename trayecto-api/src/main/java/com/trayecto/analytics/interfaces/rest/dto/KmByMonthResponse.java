package com.trayecto.analytics.interfaces.rest.dto;

import com.trayecto.analytics.application.query.GetKmByMonthHandler.MonthlyKmPoint;

import java.math.BigDecimal;

public record KmByMonthResponse(String yearMonth, BigDecimal km) {
    public static KmByMonthResponse from(MonthlyKmPoint point) {
        return new KmByMonthResponse(point.yearMonth(), point.km());
    }
}
