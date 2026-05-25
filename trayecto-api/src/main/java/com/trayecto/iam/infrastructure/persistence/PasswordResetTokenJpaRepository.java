package com.trayecto.iam.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    Optional<PasswordResetTokenEntity> findByHashedToken(String hashedToken);

    @Modifying
    @Query("update PasswordResetTokenEntity t set t.consumedAt = :at " +
           "where t.userId = :userId and t.consumedAt is null")
    int invalidatePreviousFor(@Param("userId") UUID userId, @Param("at") Instant at);

    @Modifying
    @Query("delete from PasswordResetTokenEntity t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
