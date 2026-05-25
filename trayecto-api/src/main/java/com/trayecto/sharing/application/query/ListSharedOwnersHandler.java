package com.trayecto.sharing.application.query;

import com.trayecto.iam.api.IamPublicApi;
import com.trayecto.iam.api.dto.UserSnapshot;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.sharing.domain.AccessGrantRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Lista de owners cuyos viajes el usuario actual puede ver (los que aceptó como grantee).
 */
@Component
public class ListSharedOwnersHandler {

    public record Query(UserId granteeId) {}

    private final AccessGrantRepository repository;
    private final IamPublicApi iam;

    public ListSharedOwnersHandler(AccessGrantRepository repository, IamPublicApi iam) {
        this.repository = repository;
        this.iam = iam;
    }

    @Transactional(readOnly = true)
    public List<UserSnapshot> handle(Query query) {
        return repository.findActiveOwnersFor(query.granteeId()).stream()
            .map(iam::findUserSnapshot)
            .flatMap(Optional::stream)
            .toList();
    }
}
