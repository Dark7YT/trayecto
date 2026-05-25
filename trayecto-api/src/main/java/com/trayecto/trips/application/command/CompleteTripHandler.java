package com.trayecto.trips.application.command;

import com.trayecto.shared.kernel.Kilometers;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.shared.kernel.exception.NotFoundException;
import com.trayecto.trips.domain.MultiplierId;
import com.trayecto.trips.domain.Trip;
import com.trayecto.shared.kernel.TripId;
import com.trayecto.trips.domain.TripRepository;
import com.trayecto.trips.domain.UserMultiplier;
import com.trayecto.trips.domain.UserMultiplierRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class CompleteTripHandler {

    public record Command(
        UserId userId,
        TripId tripId,
        BigDecimal endKm,
        MultiplierId multiplierId,
        String endPhotoUrl
    ) {}

    private final TripRepository tripRepository;
    private final UserMultiplierRepository multiplierRepository;
    private final ApplicationEventPublisher events;

    public CompleteTripHandler(
        TripRepository tripRepository,
        UserMultiplierRepository multiplierRepository,
        ApplicationEventPublisher events
    ) {
        this.tripRepository = tripRepository;
        this.multiplierRepository = multiplierRepository;
        this.events = events;
    }

    @Transactional
    public void handle(Command command) {
        Trip trip = tripRepository.findById(command.tripId())
            .orElseThrow(() -> new NotFoundException("trip.not_found", "Trip not found"));
        if (!trip.userId().equals(command.userId())) {
            throw new BusinessRuleViolation("trip.not_owner", "Trip does not belong to current user");
        }
        UserMultiplier multiplier = multiplierRepository.findById(command.multiplierId())
            .orElseThrow(() -> new NotFoundException("multiplier.not_found", "Multiplier not found"));
        if (!multiplier.userId().equals(command.userId())) {
            throw new BusinessRuleViolation("multiplier.not_owner",
                "Multiplier does not belong to current user");
        }
        trip.complete(
            Kilometers.of(command.endKm()),
            multiplier.id(),
            multiplier.value(),
            command.endPhotoUrl()
        );
        tripRepository.save(trip);
        trip.pullDomainEvents().forEach(events::publishEvent);
    }
}
