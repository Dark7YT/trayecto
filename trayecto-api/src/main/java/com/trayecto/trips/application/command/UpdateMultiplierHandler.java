package com.trayecto.trips.application.command;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.shared.kernel.exception.NotFoundException;
import com.trayecto.trips.domain.MultiplierId;
import com.trayecto.trips.domain.UserMultiplier;
import com.trayecto.trips.domain.UserMultiplierRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class UpdateMultiplierHandler {

    public record Command(UserId userId, MultiplierId id, String name, BigDecimal value) {}

    private final UserMultiplierRepository repository;

    public UpdateMultiplierHandler(UserMultiplierRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void handle(Command command) {
        UserMultiplier multiplier = repository.findById(command.id())
            .orElseThrow(() -> new NotFoundException("multiplier.not_found", "Multiplier not found"));
        if (!multiplier.userId().equals(command.userId())) {
            throw new BusinessRuleViolation("multiplier.not_owner",
                "Multiplier does not belong to current user");
        }
        // Si renombramos a un nombre existente (que no es el mismo), fallar.
        String newName = command.name().trim();
        if (!multiplier.name().equals(newName)
            && repository.existsByUserAndName(command.userId(), newName)) {
            throw new BusinessRuleViolation("multiplier.name_duplicate",
                "A multiplier with this name already exists");
        }
        multiplier.update(newName, command.value());
        repository.save(multiplier);
    }
}
