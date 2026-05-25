package com.trayecto.trips.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface UserMultiplierJpaRepository extends JpaRepository<UserMultiplierEntity, UUID> {

    List<UserMultiplierEntity> findByUserIdOrderByIsDefaultDescNameAsc(UUID userId);

    Optional<UserMultiplierEntity> findByUserIdAndIsDefaultTrue(UUID userId);

    long countByUserId(UUID userId);

    boolean existsByUserIdAndName(UUID userId, String name);
}
