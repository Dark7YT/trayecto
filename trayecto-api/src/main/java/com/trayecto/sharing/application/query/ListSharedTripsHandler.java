package com.trayecto.sharing.application.query;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.sharing.domain.AccessGrantRepository;
import com.trayecto.trips.api.TripsPublicApi;
import com.trayecto.trips.api.dto.TripSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lista los viajes PENDING + COMPLETED de un owner — solo si el viewer tiene acceso
 * ACEPTADO. Los cancelados quedan privados.
 */
@Component
public class ListSharedTripsHandler {

    public record Query(UserId viewerId, UserId ownerId) {}

    private final AccessGrantRepository accessRepo;
    private final TripsPublicApi tripsApi;

    public ListSharedTripsHandler(AccessGrantRepository accessRepo, TripsPublicApi tripsApi) {
        this.accessRepo = accessRepo;
        this.tripsApi = tripsApi;
    }

    @Transactional(readOnly = true)
    public List<TripSnapshot> handle(Query query) {
        if (!accessRepo.hasActiveAccess(query.ownerId(), query.viewerId())) {
            throw new BusinessRuleViolation("access_grant.no_access",
                "You don't have access to this user's trips");
        }
        return tripsApi.listSharedByUser(query.ownerId());
    }
}
