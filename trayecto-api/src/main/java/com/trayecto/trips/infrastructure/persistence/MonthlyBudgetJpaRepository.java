package com.trayecto.trips.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MonthlyBudgetJpaRepository extends JpaRepository<MonthlyBudgetEntity, UUID> {

    Optional<MonthlyBudgetEntity> findByUserIdAndYearAndMonth(UUID userId, int year, int month);

    List<MonthlyBudgetEntity> findByUserIdOrderByYearDescMonthDesc(UUID userId);
}
