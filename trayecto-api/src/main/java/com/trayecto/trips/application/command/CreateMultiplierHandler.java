package com.trayecto.trips.application.command;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.trips.domain.MultiplierId;
import com.trayecto.trips.domain.UserMultiplier;
import com.trayecto.trips.domain.UserMultiplierRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class CreateMultiplierHandler {

    public record Command(UserId userId, String name, BigDecimal value, boolean asDefault) {}
    public record Result(MultiplierId id) {}

    private final UserMultiplierRepository repository;

    public CreateMultiplierHandler(UserMultiplierRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Result handle(Command command) {
        if (repository.countByUser(command.userId()) >= UserMultiplier.MAX_PER_USER) {
            throw new BusinessRuleViolation("multiplier.max_reached",
                "Maximum of " + UserMultiplier.MAX_PER_USER + " multipliers per user");
        }
        if (repository.existsByUserAndName(command.userId(), command.name().trim())) {
            throw new BusinessRuleViolation("multiplier.name_duplicate",
                "A multiplier with this name already exists");
        }

        // Si es el primero del usuario, lo marcamos default automáticamente.
        boolean shouldBeDefault = command.asDefault() || repository.countByUser(command.userId()) == 0;
        if (shouldBeDefault) {
            repository.findDefaultByUser(command.userId()).ifPresent(existing -> {
                existing.demoteFromDefault();
                repository.save(existing);
            });
        }

        UserMultiplier multiplier = UserMultiplier.create(
            command.userId(), command.name(), command.value(), shouldBeDefault
        );
        UserMultiplier saved = repository.save(multiplier);
        return new Result(saved.id());
    }
}
