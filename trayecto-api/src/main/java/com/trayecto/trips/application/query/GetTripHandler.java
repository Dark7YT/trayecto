package com.trayecto.trips.application.query;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.shared.kernel.exception.NotFoundException;
import com.trayecto.trips.domain.Trip;
import com.trayecto.shared.kernel.TripId;
import com.trayecto.trips.domain.TripRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GetTripHandler {

    public record Query(UserId userId, TripId tripId) {}

    private final TripRepository repository;

    public GetTripHandler(TripRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Trip handle(Query query) {
        Trip trip = repository.findById(query.tripId())
            .orElseThrow(() -> new NotFoundException("trip.not_found", "Trip not found"));
        if (!trip.userId().equals(query.userId())) {
            throw new BusinessRuleViolation("trip.not_owner", "Trip does not belong to current user");
        }
        return trip;
    }
}
