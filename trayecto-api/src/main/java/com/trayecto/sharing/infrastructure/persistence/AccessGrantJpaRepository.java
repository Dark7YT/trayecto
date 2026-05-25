package com.trayecto.sharing.infrastructure.persistence;

import com.trayecto.sharing.domain.AccessGrantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AccessGrantJpaRepository extends JpaRepository<AccessGrantEntity, UUID> {

    Optional<AccessGrantEntity> findByInviteTokenHash(String hash);

    List<AccessGrantEntity> findByOwnerIdOrderByInvitedAtDesc(UUID ownerId);

    List<AccessGrantEntity> findByGranteeEmailOrderByInvitedAtDesc(String granteeEmail);

    @Query("""
        select g from AccessGrantEntity g
        where g.granteeId = :granteeId
          and g.status = com.trayecto.sharing.domain.AccessGrantStatus.ACCEPTED
        order by g.respondedAt desc
        """)
    List<AccessGrantEntity> findActiveAccessForGrantee(@Param("granteeId") UUID granteeId);

    @Query("""
        select distinct g.ownerId from AccessGrantEntity g
        where g.granteeId = :granteeId
          and g.status = com.trayecto.sharing.domain.AccessGrantStatus.ACCEPTED
        """)
    List<UUID> findActiveOwnerIdsFor(@Param("granteeId") UUID granteeId);

    boolean existsByOwnerIdAndGranteeIdAndStatus(UUID ownerId, UUID granteeId, AccessGrantStatus status);

    @Query("""
        select count(g) > 0 from AccessGrantEntity g
        where g.ownerId = :ownerId
          and lower(g.granteeEmail) = lower(:granteeEmail)
          and g.status in (com.trayecto.sharing.domain.AccessGrantStatus.PENDING,
                           com.trayecto.sharing.domain.AccessGrantStatus.ACCEPTED)
        """)
    boolean existsActiveInvite(@Param("ownerId") UUID ownerId, @Param("granteeEmail") String granteeEmail);

    @Query("""
        select g.granteeId from AccessGrantEntity g
        where g.ownerId = :ownerId
          and g.granteeId is not null
          and g.status = com.trayecto.sharing.domain.AccessGrantStatus.ACCEPTED
        """)
    List<UUID> findActiveGranteeIdsFor(@Param("ownerId") UUID ownerId);
}
