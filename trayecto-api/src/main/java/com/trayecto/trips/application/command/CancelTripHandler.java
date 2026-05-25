package com.trayecto.trips.application.command;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.shared.kernel.exception.NotFoundException;
import com.trayecto.trips.domain.Trip;
import com.trayecto.shared.kernel.TripId;
import com.trayecto.trips.domain.TripRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CancelTripHandler {

    public record Command(UserId userId, TripId tripId) {}

    private final TripRepository tripRepository;
    private final ApplicationEventPublisher events;

    public CancelTripHandler(TripRepository tripRepository, ApplicationEventPublisher events) {
        this.tripRepository = tripRepository;
        this.events = events;
    }

    @Transactional
    public void handle(Command command) {
        Trip trip = tripRepository.findById(command.tripId())
            .orElseThrow(() -> new NotFoundException("trip.not_found", "Trip not found"));
        if (!trip.userId().equals(command.userId())) {
            throw new BusinessRuleViolation("trip.not_owner", "Trip does not belong to current user");
        }
        trip.cancel();
        tripRepository.save(trip);
        trip.pullDomainEvents().forEach(events::publishEvent);
    }
}
