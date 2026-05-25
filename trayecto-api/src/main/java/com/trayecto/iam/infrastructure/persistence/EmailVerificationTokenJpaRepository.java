package com.trayecto.iam.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationTokenEntity, UUID> {

    Optional<EmailVerificationTokenEntity> findByHashedToken(String hashedToken);

    @Modifying
    @Query("update EmailVerificationTokenEntity t set t.consumedAt = :at " +
           "where t.userId = :userId and t.consumedAt is null")
    int invalidatePreviousFor(@Param("userId") UUID userId, @Param("at") Instant at);

    @Modifying
    @Query("delete from EmailVerificationTokenEntity t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
