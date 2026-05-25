package com.trayecto.trips.infrastructure.persistence;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.domain.MonthlyBudget;
import com.trayecto.trips.domain.MonthlyBudgetRepository;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Component
class MonthlyBudgetRepositoryAdapter implements MonthlyBudgetRepository {

    private final MonthlyBudgetJpaRepository jpa;

    MonthlyBudgetRepositoryAdapter(MonthlyBudgetJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<MonthlyBudget> find(UserId userId, YearMonth period) {
        return jpa.findByUserIdAndYearAndMonth(userId.value(), period.getYear(), period.getMonthValue())
            .map(MonthlyBudgetMapper::toDomain);
    }

    @Override
    public List<MonthlyBudget> findByUser(UserId userId) {
        return jpa.findByUserIdOrderByYearDescMonthDesc(userId.value())
            .stream().map(MonthlyBudgetMapper::toDomain).toList();
    }

    @Override
    public MonthlyBudget save(MonthlyBudget budget) {
        MonthlyBudgetEntity existing = jpa.findById(budget.id()).orElse(null);
        MonthlyBudgetEntity entity = MonthlyBudgetMapper.toEntity(budget, existing);
        return MonthlyBudgetMapper.toDomain(jpa.save(entity));
    }
}
