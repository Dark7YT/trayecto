package com.trayecto.trips.application.command;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.shared.kernel.exception.NotFoundException;
import com.trayecto.trips.domain.MultiplierId;
import com.trayecto.trips.domain.UserMultiplier;
import com.trayecto.trips.domain.UserMultiplierRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DeleteMultiplierHandler {

    public record Command(UserId userId, MultiplierId id) {}

    private final UserMultiplierRepository repository;

    public DeleteMultiplierHandler(UserMultiplierRepository repository) {
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
        repository.delete(multiplier);

        // Si era el default, promover otro automáticamente (si queda alguno).
        if (multiplier.isDefault()) {
            List<UserMultiplier> remaining = repository.findByUser(command.userId());
            if (!remaining.isEmpty()) {
                UserMultiplier newDefault = remaining.getFirst();
                newDefault.promoteAsDefault();
                repository.save(newDefault);
            }
        }
    }
}
