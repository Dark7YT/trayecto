package com.trayecto.sharing.application;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.sharing.api.SharingPublicApi;
import com.trayecto.sharing.api.dto.AccessGrantSnapshot;
import com.trayecto.sharing.domain.AccessGrant;
import com.trayecto.sharing.domain.AccessGrantRepository;
import com.trayecto.shared.kernel.TripId;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class SharingPublicApiAdapter implements SharingPublicApi {

    private final AccessGrantRepository repository;

    SharingPublicApiAdapter(AccessGrantRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean canViewTripsOf(UserId viewerId, UserId ownerId) {
        if (viewerId.equals(ownerId)) return true;
        return repository.hasActiveAccess(ownerId, viewerId);
    }

    @Override
    public List<UserId> findActiveGranteesOf(UserId ownerId) {
        return repository.findSentByOwner(ownerId).stream()
            .filter(g -> g.status().isActive())
            .map(AccessGrant::granteeId)
            .flatMap(java.util.Optional::stream)
            .toList();
    }

    @Override
    public List<AccessGrantSnapshot> findActiveGrantsFor(UserId granteeId) {
        return repository.findActiveAccessFor(granteeId).stream()
            .map(SharingPublicApiAdapter::toSnapshot).toList();
    }

    @Override
    public boolean canCommentOnTrip(UserId userId, TripId tripId, UserId tripOwnerId) {
        if (userId.equals(tripOwnerId)) return true;
        return repository.hasActiveAccess(tripOwnerId, userId);
    }

    private static AccessGrantSnapshot toSnapshot(AccessGrant g) {
        return new AccessGrantSnapshot(
            g.id(),
            g.ownerId(),
            g.granteeEmail(),
            g.granteeId().orElse(null),
            g.status(),
            g.invitedAt(),
            g.respondedAt().orElse(null),
            g.revokedAt().orElse(null)
        );
    }
}
