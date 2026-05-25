package com.trayecto.sharing.application.command;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.shared.kernel.exception.NotFoundException;
import com.trayecto.sharing.domain.AccessGrant;
import com.trayecto.sharing.domain.AccessGrantId;
import com.trayecto.sharing.domain.AccessGrantRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RevokeAccessHandler {

    public record Command(UserId ownerId, AccessGrantId grantId) {}

    private final AccessGrantRepository repository;
    private final ApplicationEventPublisher events;

    public RevokeAccessHandler(AccessGrantRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional
    public void handle(Command command) {
        AccessGrant grant = repository.findById(command.grantId())
            .orElseThrow(() -> new NotFoundException("access_grant.not_found", "Invitation not found"));
        if (!grant.ownerId().equals(command.ownerId())) {
            throw new BusinessRuleViolation("access_grant.not_owner",
                "Only the owner can revoke this invitation");
        }
        grant.revoke();
        repository.save(grant);
        grant.pullDomainEvents().forEach(events::publishEvent);
    }
}
