package com.trayecto.sharing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface TripCommentJpaRepository extends JpaRepository<TripCommentEntity, UUID> {

    @Query("""
        select c from TripCommentEntity c
        where c.tripId = :tripId
          and c.deletedAt is null
        order by c.createdAt asc
        """)
    List<TripCommentEntity> findByTripVisible(@Param("tripId") UUID tripId);

    @Query("""
        select count(c) from TripCommentEntity c
        where c.tripId = :tripId
          and c.deletedAt is null
        """)
    long countByTripVisible(@Param("tripId") UUID tripId);
}
