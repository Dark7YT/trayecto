package com.trayecto.trips.infrastructure.persistence;

import com.trayecto.trips.domain.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface TripJpaRepository extends JpaRepository<TripEntity, UUID> {

    // Métodos divididos por combinación de filtros para evitar el patrón
    // ":param is null or ..." que rompe en PostgreSQL cuando el parámetro
    // es null y el tipo Java no puede inferirse (error 42P18 "could not
    // determine data type"). El adapter elige cuál llamar según los filtros.

    @Query("""
        select t from TripEntity t
        where t.userId = :userId
          and t.deletedAt is null
        order by t.startedAt desc
        """)
    List<TripEntity> findByUserId(@Param("userId") UUID userId);

    @Query("""
        select t from TripEntity t
        where t.userId = :userId
          and t.status = :status
          and t.deletedAt is null
        order by t.startedAt desc
        """)
    List<TripEntity> findByUserIdAndStatus(
        @Param("userId") UUID userId,
        @Param("status") TripStatus status
    );

    @Query("""
        select t from TripEntity t
        where t.userId = :userId
          and t.startedAt >= :fromInclusive
          and t.startedAt < :toExclusive
          and t.deletedAt is null
        order by t.startedAt desc
        """)
    List<TripEntity> findByUserIdAndDateRange(
        @Param("userId") UUID userId,
        @Param("fromInclusive") Instant fromInclusive,
        @Param("toExclusive") Instant toExclusive
    );

    @Query("""
        select t from TripEntity t
        where t.userId = :userId
          and t.status = :status
          and t.startedAt >= :fromInclusive
          and t.startedAt < :toExclusive
          and t.deletedAt is null
        order by t.startedAt desc
        """)
    List<TripEntity> findByUserIdAndStatusAndDateRange(
        @Param("userId") UUID userId,
        @Param("status") TripStatus status,
        @Param("fromInclusive") Instant fromInclusive,
        @Param("toExclusive") Instant toExclusive
    );

    @Query("""
        select t from TripEntity t
        where t.userId = :userId
          and t.status = com.trayecto.trips.domain.TripStatus.COMPLETED
          and t.deletedAt is null
        order by t.completedAt desc
        """)
    List<TripEntity> findAllCompletedByUser(@Param("userId") UUID userId);

    @Query("""
        select t from TripEntity t
        where t.userId = :userId
          and t.status = com.trayecto.trips.domain.TripStatus.COMPLETED
          and t.completedAt >= :fromInclusive
          and t.completedAt < :toExclusive
          and t.deletedAt is null
        order by t.completedAt desc
        """)
    List<TripEntity> findCompletedByUserAndDateRange(
        @Param("userId") UUID userId,
        @Param("fromInclusive") Instant fromInclusive,
        @Param("toExclusive") Instant toExclusive
    );

    @Query("""
        select t from TripEntity t
        where t.userId = :userId
          and t.status in (com.trayecto.trips.domain.TripStatus.PENDING,
                           com.trayecto.trips.domain.TripStatus.COMPLETED)
          and t.deletedAt is null
        order by t.startedAt desc
        """)
    List<TripEntity> findSharedByUser(@Param("userId") UUID userId);

    long countByUserIdAndStatusAndDeletedAtIsNull(UUID userId, TripStatus status);
}
