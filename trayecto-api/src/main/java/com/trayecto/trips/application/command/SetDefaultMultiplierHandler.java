package com.trayecto.trips.application.command;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.shared.kernel.exception.NotFoundException;
import com.trayecto.trips.domain.MultiplierId;
import com.trayecto.trips.domain.UserMultiplier;
import com.trayecto.trips.domain.UserMultiplierRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SetDefaultMultiplierHandler {

    public record Command(UserId userId, MultiplierId id) {}

    private final UserMultiplierRepository repository;

    public SetDefaultMultiplierHandler(UserMultiplierRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void handle(Command command) {
        UserMultiplier target = repository.findById(command.id())
            .orElseThrow(() -> new NotFoundException("multiplier.not_found", "Multiplier not found"));
        if (!target.userId().equals(command.userId())) {
            throw new BusinessRuleViolation("multiplier.not_owner",
                "Multiplier does not belong to current user");
        }
        if (target.isDefault()) return; // ya es default
        repository.findDefaultByUser(command.userId()).ifPresent(currentDefault -> {
            currentDefault.demoteFromDefault();
            repository.save(currentDefault);
        });
        target.promoteAsDefault();
        repository.save(target);
    }
}
