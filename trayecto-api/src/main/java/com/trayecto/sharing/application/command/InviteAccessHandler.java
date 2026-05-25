package com.trayecto.sharing.application.command;

import com.trayecto.iam.api.IamPublicApi;
import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.OpaqueTokens;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.sharing.domain.AccessGrant;
import com.trayecto.sharing.domain.AccessGrantId;
import com.trayecto.sharing.domain.AccessGrantRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InviteAccessHandler {

    public record Command(UserId ownerId, String granteeEmailRaw, String recaptchaToken) {}
    public record Result(AccessGrantId id) {}

    private final AccessGrantRepository repository;
    private final IamPublicApi iam;
    private final ApplicationEventPublisher events;

    public InviteAccessHandler(
        AccessGrantRepository repository,
        IamPublicApi iam,
        ApplicationEventPublisher events
    ) {
        this.repository = repository;
        this.iam = iam;
        this.events = events;
    }

    @Transactional
    public Result handle(Command command) {
        Email granteeEmail = Email.of(command.granteeEmailRaw());

        // Owner no puede invitarse a sí mismo
        var ownerSnapshot = iam.findUserSnapshot(command.ownerId())
            .orElseThrow(() -> new BusinessRuleViolation("user.not_found", "Owner not found"));
        if (ownerSnapshot.email().equals(granteeEmail)) {
            throw new BusinessRuleViolation("access_grant.self_invite",
                "Cannot invite yourself");
        }

        if (repository.existsActiveInvite(command.ownerId(), granteeEmail)) {
            throw new BusinessRuleViolation("access_grant.duplicate_invite",
                "There is already a pending or accepted invitation for that email");
        }

        String tokenRaw = OpaqueTokens.randomRaw();
        String tokenHash = OpaqueTokens.sha256Hex(tokenRaw);
        AccessGrant grant = AccessGrant.invite(command.ownerId(), granteeEmail, tokenRaw, tokenHash);
        repository.save(grant);
        grant.pullDomainEvents().forEach(events::publishEvent);
        return new Result(grant.id());
    }
}
