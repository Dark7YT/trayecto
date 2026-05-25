package com.trayecto.sharing.infrastructure.persistence;

import com.trayecto.sharing.domain.CommentId;
import com.trayecto.sharing.domain.TripComment;
import com.trayecto.sharing.domain.TripCommentRepository;
import com.trayecto.shared.kernel.TripId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class TripCommentRepositoryAdapter implements TripCommentRepository {

    private final TripCommentJpaRepository jpa;

    TripCommentRepositoryAdapter(TripCommentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<TripComment> findById(CommentId id) {
        return jpa.findById(id.value()).map(TripCommentMapper::toDomain);
    }

    @Override
    public List<TripComment> findByTrip(TripId tripId) {
        return jpa.findByTripVisible(tripId.value())
            .stream().map(TripCommentMapper::toDomain).toList();
    }

    @Override
    public long countByTrip(TripId tripId) {
        return jpa.countByTripVisible(tripId.value());
    }

    @Override
    public TripComment save(TripComment comment) {
        TripCommentEntity existing = jpa.findById(comment.id().value()).orElse(null);
        TripCommentEntity entity = TripCommentMapper.toEntity(comment, existing);
        return TripCommentMapper.toDomain(jpa.save(entity));
    }
}
