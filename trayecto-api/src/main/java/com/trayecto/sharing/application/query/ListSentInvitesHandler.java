package com.trayecto.sharing.application.query;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.sharing.domain.AccessGrant;
import com.trayecto.sharing.domain.AccessGrantRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListSentInvitesHandler {

    public record Query(UserId ownerId) {}

    private final AccessGrantRepository repository;

    public ListSentInvitesHandler(AccessGrantRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AccessGrant> handle(Query query) {
        return repository.findSentByOwner(query.ownerId());
    }
}
