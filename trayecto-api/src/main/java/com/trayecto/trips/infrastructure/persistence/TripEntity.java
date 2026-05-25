package com.trayecto.trips.infrastructure.persistence;

import com.trayecto.trips.domain.TripStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "trips_trips",
    indexes = {
        @Index(name = "ix_trips_user_started", columnList = "user_id,started_at"),
        @Index(name = "ix_trips_user_status",  columnList = "user_id,status"),
        @Index(name = "ix_trips_completed_at", columnList = "completed_at")
    }
)
class TripEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "name", nullable = false, length = 100)
    String name;

    @Column(name = "start_km", nullable = false, precision = 8, scale = 1)
    BigDecimal startKm;

    @Column(name = "end_km", precision = 8, scale = 1)
    BigDecimal endKm;

    @Column(name = "multiplier_id")
    UUID multiplierId;

    @Column(name = "multiplier_rate", precision = 4, scale = 2)
    BigDecimal multiplierRate;

    @Column(name = "total_cost_amount", precision = 12, scale = 2)
    BigDecimal totalCostAmount;

    @Column(name = "total_cost_currency", length = 3)
    String totalCostCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    TripStatus status;

    @Column(name = "start_photo_url", length = 512)
    String startPhotoUrl;

    @Column(name = "end_photo_url", length = 512)
    String endPhotoUrl;

    @Column(name = "started_at", nullable = false)
    Instant startedAt;

    @Column(name = "completed_at")
    Instant completedAt;

    @Column(name = "cancelled_at")
    Instant cancelledAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Column(name = "deleted_at")
    Instant deletedAt;

    TripEntity() {}
}
