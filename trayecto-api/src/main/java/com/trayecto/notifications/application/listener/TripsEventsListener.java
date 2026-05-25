package com.trayecto.notifications.application.listener;

import com.trayecto.notifications.api.NotificationType;
import com.trayecto.notifications.application.NotificationDispatcher;
import com.trayecto.trips.api.events.BudgetThresholdReached;
import com.trayecto.trips.api.events.TripCompleted;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class TripsEventsListener {

    private final NotificationDispatcher dispatcher;

    TripsEventsListener(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @ApplicationModuleListener
    void onTripCompleted(TripCompleted event) {
        dispatcher.dispatch(
            event.userId(),
            NotificationType.TRIP_COMPLETED,
            String.format("Viaje cerrado: S/ %.2f (%.1f km)",
                event.totalCost().amount(), event.distanceKm()),
            Map.of(
                "tripId", event.tripId().asString(),
                "distanceKm", event.distanceKm().toString(),
                "totalCost", event.totalCost().amount().toString(),
                "multiplierRate", event.multiplierRate().toString()
            )
        );
    }

    @ApplicationModuleListener
    void onBudgetThresholdReached(BudgetThresholdReached event) {
        NotificationType type = event.thresholdPercent() >= 100
            ? NotificationType.BUDGET_EXCEEDED
            : NotificationType.BUDGET_WARNING;
        String title = event.thresholdPercent() >= 100
            ? String.format("Presupuesto de %s excedido", event.period())
            : String.format("Ya usaste el %d%% del presupuesto de %s",
                event.thresholdPercent(), event.period());

        dispatcher.dispatch(
            event.userId(),
            type,
            title,
            Map.of(
                "period", event.period().toString(),
                "thresholdPercent", event.thresholdPercent(),
                "currentSpend", event.currentSpend().amount().toString(),
                "budgetAmount", event.budgetAmount().amount().toString()
            )
        );
    }
}
