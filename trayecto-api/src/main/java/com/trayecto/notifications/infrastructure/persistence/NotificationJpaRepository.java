package com.trayecto.notifications.infrastructure.persistence;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Limit limit);

    long countByUserIdAndReadAtIsNull(UUID userId);

    @Modifying
    @Query("update NotificationEntity n set n.readAt = :at " +
           "where n.userId = :userId and n.readAt is null")
    int markAllAsRead(@Param("userId") UUID userId, @Param("at") Instant at);

    @Modifying
    @Query("delete from NotificationEntity n where n.readAt is not null and n.readAt < :cutoff")
    int deleteReadOlderThan(@Param("cutoff") Instant cutoff);
}
