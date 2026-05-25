package com.trayecto.trips.application.command;

import com.trayecto.shared.kernel.Money;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.domain.MonthlyBudget;
import com.trayecto.trips.domain.MonthlyBudgetRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Upsert del presupuesto mensual. Si ya existe, actualiza el monto y resetea las flags
 * de umbrales si el nuevo monto es mayor al spend actual.
 */
@Component
public class SetMonthlyBudgetHandler {

    public record Command(UserId userId, YearMonth period, BigDecimal amount) {}

    private final MonthlyBudgetRepository repository;
    private final ApplicationEventPublisher events;

    public SetMonthlyBudgetHandler(MonthlyBudgetRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional
    public void handle(Command command) {
        Money amount = Money.pen(command.amount());
        MonthlyBudget budget = repository.find(command.userId(), command.period())
            .orElseGet(() -> MonthlyBudget.create(command.userId(), command.period(), amount));

        if (budget.amount().amount().compareTo(amount.amount()) != 0) {
            budget.updateAmount(amount);
        }
        repository.save(budget);
        budget.pullDomainEvents().forEach(events::publishEvent);
    }
}
