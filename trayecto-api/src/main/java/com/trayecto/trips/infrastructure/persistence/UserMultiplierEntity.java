package com.trayecto.trips.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "trips_user_multipliers",
    indexes = @Index(name = "ix_trips_mult_user", columnList = "user_id"),
    uniqueConstraints = @UniqueConstraint(
        name = "ux_trips_mult_user_name",
        columnNames = {"user_id", "name"}
    )
)
class UserMultiplierEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "name", nullable = false, length = 50)
    String name;

    @Column(name = "value", nullable = false, precision = 4, scale = 2)
    BigDecimal value;

    @Column(name = "is_default", nullable = false)
    boolean isDefault;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    UserMultiplierEntity() {}
}
