package com.trayecto.trips.application.listener;

import com.trayecto.trips.api.events.TripCompleted;
import com.trayecto.trips.domain.MonthlyBudget;
import com.trayecto.trips.domain.MonthlyBudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * Listener intra-módulo: cuando un viaje se completa, sumamos su costo al presupuesto del
 * mes correspondiente (basado en {@code completedAt} en la zona horaria del usuario —
 * por ahora hardcoded a America/Lima, en Fase 6 leeremos la zona del perfil).
 * <p>
 * Si {@code MonthlyBudget} no existe para ese mes, no hay nada que alertar (el usuario
 * no fijó presupuesto). Si existe y cruza umbral, emite {@code BudgetThresholdReached}
 * que escuchará {@code notifications} en Fase 4.
 */
@Component
class BudgetAlertListener {

    private static final Logger log = LoggerFactory.getLogger(BudgetAlertListener.class);
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Lima");

    private final MonthlyBudgetRepository repository;
    private final ApplicationEventPublisher events;

    BudgetAlertListener(MonthlyBudgetRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @ApplicationModuleListener
    void onTripCompleted(TripCompleted event) {
        LocalDate localDate = event.completedAt().atZone(DEFAULT_ZONE).toLocalDate();
        YearMonth period = YearMonth.from(localDate);

        MonthlyBudget budget = repository.find(event.userId(), period).orElse(null);
        if (budget == null) {
            log.debug("No budget set for user {} period {}, skipping", event.userId().asString(), period);
            return;
        }
        budget.recordSpend(event.totalCost());
        repository.save(budget);
        budget.pullDomainEvents().forEach(events::publishEvent);
    }
}
