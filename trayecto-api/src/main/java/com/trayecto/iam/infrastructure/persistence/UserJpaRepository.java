package com.trayecto.iam.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByGoogleSubject(String googleSubject);

    boolean existsByEmail(String email);
}
