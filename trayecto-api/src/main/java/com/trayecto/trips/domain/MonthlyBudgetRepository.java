package com.trayecto.trips.domain;

import com.trayecto.shared.kernel.UserId;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface MonthlyBudgetRepository {

    Optional<MonthlyBudget> find(UserId userId, YearMonth period);

    List<MonthlyBudget> findByUser(UserId userId);

    MonthlyBudget save(MonthlyBudget budget);
}
