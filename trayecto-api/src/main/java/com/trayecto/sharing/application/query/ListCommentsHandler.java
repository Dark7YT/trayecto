package com.trayecto.sharing.application.query;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.shared.kernel.exception.NotFoundException;
import com.trayecto.sharing.domain.AccessGrantRepository;
import com.trayecto.sharing.domain.TripComment;
import com.trayecto.sharing.domain.TripCommentRepository;
import com.trayecto.trips.api.TripsPublicApi;
import com.trayecto.shared.kernel.TripId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListCommentsHandler {

    public record Query(UserId viewerId, TripId tripId) {}

    private final TripCommentRepository commentRepo;
    private final AccessGrantRepository accessRepo;
    private final TripsPublicApi tripsApi;

    public ListCommentsHandler(
        TripCommentRepository commentRepo,
        AccessGrantRepository accessRepo,
        TripsPublicApi tripsApi
    ) {
        this.commentRepo = commentRepo;
        this.accessRepo = accessRepo;
        this.tripsApi = tripsApi;
    }

    @Transactional(readOnly = true)
    public List<TripComment> handle(Query query) {
        var tripSnapshot = tripsApi.findTripSnapshot(query.tripId())
            .orElseThrow(() -> new NotFoundException("trip.not_found", "Trip not found"));
        UserId ownerId = tripSnapshot.userId();

        boolean isOwner = ownerId.equals(query.viewerId());
        boolean isGrantee = accessRepo.hasActiveAccess(ownerId, query.viewerId());
        if (!isOwner && !isGrantee) {
            throw new BusinessRuleViolation("comment.not_authorized",
                "You don't have access to view comments on this trip");
        }
        return commentRepo.findByTrip(query.tripId());
    }
}
