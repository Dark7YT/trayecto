package com.trayecto.trips.application.query;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.domain.Trip;
import com.trayecto.trips.domain.TripRepository;
import com.trayecto.trips.domain.TripStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class ListTripsHandler {

    public record Query(
        UserId userId,
        TripStatus statusFilter, // nullable
        Instant fromInclusive,   // nullable
        Instant toExclusive       // nullable
    ) {}

    private final TripRepository repository;

    public ListTripsHandler(TripRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Trip> handle(Query query) {
        return repository.findByUser(
            query.userId(), query.statusFilter(), query.fromInclusive(), query.toExclusive()
        );
    }
}
