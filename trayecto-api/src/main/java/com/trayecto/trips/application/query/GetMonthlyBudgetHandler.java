package com.trayecto.trips.application.query;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.domain.MonthlyBudget;
import com.trayecto.trips.domain.MonthlyBudgetRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Optional;

@Component
public class GetMonthlyBudgetHandler {

    public record Query(UserId userId, YearMonth period) {}

    private final MonthlyBudgetRepository repository;

    public GetMonthlyBudgetHandler(MonthlyBudgetRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<MonthlyBudget> handle(Query query) {
        return repository.find(query.userId(), query.period());
    }
}
