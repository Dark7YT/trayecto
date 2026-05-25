package com.trayecto.sharing.application.query;

import com.trayecto.iam.api.IamPublicApi;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.NotFoundException;
import com.trayecto.sharing.domain.AccessGrant;
import com.trayecto.sharing.domain.AccessGrantRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListReceivedInvitesHandler {

    public record Query(UserId granteeId) {}

    private final AccessGrantRepository repository;
    private final IamPublicApi iam;

    public ListReceivedInvitesHandler(AccessGrantRepository repository, IamPublicApi iam) {
        this.repository = repository;
        this.iam = iam;
    }

    @Transactional(readOnly = true)
    public List<AccessGrant> handle(Query query) {
        var snapshot = iam.findUserSnapshot(query.granteeId())
            .orElseThrow(() -> new NotFoundException("user.not_found", "User not found"));
        return repository.findByGranteeEmail(snapshot.email());
    }
}
