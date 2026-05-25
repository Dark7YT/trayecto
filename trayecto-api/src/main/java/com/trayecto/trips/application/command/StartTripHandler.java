package com.trayecto.trips.application.command;

import com.trayecto.shared.kernel.Kilometers;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.domain.Trip;
import com.trayecto.shared.kernel.TripId;
import com.trayecto.trips.domain.TripName;
import com.trayecto.trips.domain.TripRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class StartTripHandler {

    public record Command(UserId userId, String name, BigDecimal startKm, String startPhotoUrl) {}
    public record Result(TripId tripId) {}

    private final TripRepository tripRepository;
    private final ApplicationEventPublisher events;

    public StartTripHandler(TripRepository tripRepository, ApplicationEventPublisher events) {
        this.tripRepository = tripRepository;
        this.events = events;
    }

    @Transactional
    public Result handle(Command command) {
        Trip trip = Trip.start(
            command.userId(),
            new TripName(command.name()),
            Kilometers.of(command.startKm()),
            command.startPhotoUrl()
        );
        Trip saved = tripRepository.save(trip);
        trip.pullDomainEvents().forEach(events::publishEvent);
        return new Result(saved.id());
    }
}
