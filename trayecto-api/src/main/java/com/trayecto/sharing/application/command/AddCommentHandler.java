package com.trayecto.sharing.application.command;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.shared.kernel.exception.NotFoundException;
import com.trayecto.sharing.domain.AccessGrantRepository;
import com.trayecto.sharing.domain.CommentBody;
import com.trayecto.sharing.domain.CommentId;
import com.trayecto.sharing.domain.TripComment;
import com.trayecto.sharing.domain.TripCommentRepository;
import com.trayecto.trips.api.TripsPublicApi;
import com.trayecto.shared.kernel.TripId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AddCommentHandler {

    public record Command(UserId authorId, TripId tripId, String body) {}
    public record Result(CommentId commentId) {}

    private final TripCommentRepository commentRepository;
    private final AccessGrantRepository accessGrantRepository;
    private final TripsPublicApi tripsApi;
    private final ApplicationEventPublisher events;

    public AddCommentHandler(
        TripCommentRepository commentRepository,
        AccessGrantRepository accessGrantRepository,
        TripsPublicApi tripsApi,
        ApplicationEventPublisher events
    ) {
        this.commentRepository = commentRepository;
        this.accessGrantRepository = accessGrantRepository;
        this.tripsApi = tripsApi;
        this.events = events;
    }

    @Transactional
    public Result handle(Command command) {
        var tripSnapshot = tripsApi.findTripSnapshot(command.tripId())
            .orElseThrow(() -> new NotFoundException("trip.not_found", "Trip not found"));
        UserId tripOwner = tripSnapshot.userId();

        boolean isOwner = tripOwner.equals(command.authorId());
        boolean isGrantee = accessGrantRepository.hasActiveAccess(tripOwner, command.authorId());

        if (!isOwner && !isGrantee) {
            throw new BusinessRuleViolation("comment.not_authorized",
                "You don't have access to comment on this trip");
        }

        TripComment comment = TripComment.create(
            command.tripId(), tripOwner, command.authorId(), new CommentBody(command.body())
        );
        commentRepository.save(comment);
        comment.pullDomainEvents().forEach(events::publishEvent);
        return new Result(comment.id());
    }
}
