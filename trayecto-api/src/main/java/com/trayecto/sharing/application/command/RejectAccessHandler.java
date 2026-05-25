package com.trayecto.sharing.application.command;

import com.trayecto.iam.api.IamPublicApi;
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
public class RejectAccessHandler {

    public record Command(UserId rejectingUserId, AccessGrantId grantId) {}

    private final AccessGrantRepository repository;
    private final IamPublicApi iam;
    private final ApplicationEventPublisher events;

    public RejectAccessHandler(
        AccessGrantRepository repository, IamPublicApi iam, ApplicationEventPublisher events
    ) {
        this.repository = repository;
        this.iam = iam;
        this.events = events;
    }

    @Transactional
    public void handle(Command command) {
        AccessGrant grant = repository.findById(command.grantId())
            .orElseThrow(() -> new NotFoundException("access_grant.not_found", "Invitation not found"));

        var userSnapshot = iam.findUserSnapshot(command.rejectingUserId())
            .orElseThrow(() -> new NotFoundException("user.not_found", "User not found"));
        if (!userSnapshot.email().equals(grant.granteeEmail())) {
            throw new BusinessRuleViolation("access_grant.email_mismatch",
                "This invitation is not for your account");
        }

        grant.reject(command.rejectingUserId());
        repository.save(grant);
        grant.pullDomainEvents().forEach(events::publishEvent);
    }
}
