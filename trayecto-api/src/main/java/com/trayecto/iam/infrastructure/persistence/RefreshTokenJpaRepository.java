package com.trayecto.iam.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByHashedToken(String hashedToken);

    List<RefreshTokenEntity> findByUserIdAndRevokedAtIsNull(UUID userId);

    List<RefreshTokenEntity> findByFamilyId(UUID familyId);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revokedAt = :at " +
           "where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("at") Instant at);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revokedAt = :at " +
           "where t.userId = :userId and t.revokedAt is null")
    int revokeAllForUser(@Param("userId") UUID userId, @Param("at") Instant at);

    @Modifying
    @Query("delete from RefreshTokenEntity t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
