package com.trayecto.trips.application;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.api.TripsPublicApi;
import com.trayecto.trips.api.dto.MultiplierSnapshot;
import com.trayecto.trips.api.dto.TripSnapshot;
import com.trayecto.trips.domain.Trip;
import com.trayecto.shared.kernel.TripId;
import com.trayecto.trips.domain.TripRepository;
import com.trayecto.trips.domain.UserMultiplierRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
class TripsPublicApiAdapter implements TripsPublicApi {

    private final TripRepository repository;
    private final UserMultiplierRepository multiplierRepository;

    TripsPublicApiAdapter(TripRepository repository, UserMultiplierRepository multiplierRepository) {
        this.repository = repository;
        this.multiplierRepository = multiplierRepository;
    }

    @Override
    public Optional<TripSnapshot> findTripSnapshot(TripId tripId) {
        return repository.findById(tripId).map(TripsPublicApiAdapter::toSnapshot);
    }

    @Override
    public List<TripSnapshot> listCompletedByUser(UserId ownerId, Instant from, Instant to) {
        return repository.findCompletedByUser(ownerId, from, to).stream()
            .map(TripsPublicApiAdapter::toSnapshot).toList();
    }

    @Override
    public List<TripSnapshot> listSharedByUser(UserId ownerId) {
        return repository.findSharedByUser(ownerId).stream()
            .map(TripsPublicApiAdapter::toSnapshot).toList();
    }

    @Override
    public List<MultiplierSnapshot> listMultipliersByUser(UserId userId) {
        return multiplierRepository.findByUser(userId).stream()
            .map(m -> new MultiplierSnapshot(
                m.id().value(),
                m.name(),
                m.value(),
                m.isDefault()
            ))
            .toList();
    }

    @Override
    public boolean isOwnedBy(TripId tripId, UserId userId) {
        return repository.findById(tripId).map(t -> t.userId().equals(userId)).orElse(false);
    }

    static TripSnapshot toSnapshot(Trip trip) {
        return new TripSnapshot(
            trip.id(),
            trip.userId(),
            trip.multiplierId().map(m -> m.value()).orElse(null),
            trip.name().value(),
            trip.startKm().value(),
            trip.endKm().map(k -> k.value()).orElse(null),
            trip.endKm().map(k -> trip.startKm().distanceTo(k).value()).orElse(null),
            trip.multiplierRate().orElse(null),
            trip.totalCost().orElse(null),
            trip.status().name(),
            trip.startPhotoUrl().orElse(null),
            trip.endPhotoUrl().orElse(null),
            trip.startedAt(),
            trip.completedAt().orElse(null),
            trip.cancelledAt().orElse(null)
        );
    }
}
