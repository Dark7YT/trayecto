package com.trayecto.trips.infrastructure.persistence;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.domain.Trip;
import com.trayecto.shared.kernel.TripId;
import com.trayecto.trips.domain.TripRepository;
import com.trayecto.trips.domain.TripStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class TripRepositoryAdapter implements TripRepository {

    private final TripJpaRepository jpa;

    TripRepositoryAdapter(TripJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Trip> findById(TripId id) {
        return jpa.findById(id.value())
            .filter(e -> e.deletedAt == null)
            .map(TripMapper::toDomain);
    }

    @Override
    public List<Trip> findByUser(UserId userId, TripStatus statusFilter,
                                  Instant fromInclusive, Instant toExclusive) {
        // Switch entre 4 queries según los filtros presentes — necesario para evitar
        // el patrón ":p is null or ..." que falla en PostgreSQL cuando :p es null
        // con tipo Java no-string (Instant, enum, etc.) — error SQLSTATE 42P18.
        UUID userIdValue = userId.value();
        boolean hasStatus = statusFilter != null;
        boolean hasRange = fromInclusive != null && toExclusive != null;
        List<TripEntity> entities;
        if (!hasStatus && !hasRange) {
            entities = jpa.findByUserId(userIdValue);
        } else if (hasStatus && !hasRange) {
            entities = jpa.findByUserIdAndStatus(userIdValue, statusFilter);
        } else if (!hasStatus && hasRange) {
            entities = jpa.findByUserIdAndDateRange(userIdValue, fromInclusive, toExclusive);
        } else {
            entities = jpa.findByUserIdAndStatusAndDateRange(
                userIdValue, statusFilter, fromInclusive, toExclusive);
        }
        return entities.stream().map(TripMapper::toDomain).toList();
    }

    @Override
    public List<Trip> findCompletedByUser(UserId userId, Instant fromInclusive, Instant toExclusive) {
        // Switch entre 2 queries según presencia de rango — mismo motivo que findByUser:
        // ":p is null or ..." con Instant null rompe en PostgreSQL (42P18).
        UUID userIdValue = userId.value();
        boolean hasRange = fromInclusive != null && toExclusive != null;
        List<TripEntity> entities = hasRange
            ? jpa.findCompletedByUserAndDateRange(userIdValue, fromInclusive, toExclusive)
            : jpa.findAllCompletedByUser(userIdValue);
        return entities.stream().map(TripMapper::toDomain).toList();
    }

    @Override
    public List<Trip> findSharedByUser(UserId userId) {
        return jpa.findSharedByUser(userId.value())
            .stream().map(TripMapper::toDomain).toList();
    }

    @Override
    public Trip save(Trip trip) {
        TripEntity existing = jpa.findById(trip.id().value()).orElse(null);
        TripEntity entity = TripMapper.toEntity(trip, existing);
        return TripMapper.toDomain(jpa.save(entity));
    }

    @Override
    public long countByUserAndStatus(UserId userId, TripStatus status) {
        return jpa.countByUserIdAndStatusAndDeletedAtIsNull(userId.value(), status);
    }
}
