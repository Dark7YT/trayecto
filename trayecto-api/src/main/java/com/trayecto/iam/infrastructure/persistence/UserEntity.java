package com.trayecto.iam.infrastructure.persistence;

import com.trayecto.iam.api.AuthProvider;
import com.trayecto.iam.domain.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iam_users")
class UserEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    String email;

    @Column(name = "password_hash", length = 72)
    String passwordHash;

    @Column(name = "display_name", nullable = false, length = 50)
    String displayName;

    @Column(name = "locale", nullable = false, length = 16)
    String locale;

    @Column(name = "timezone", nullable = false, length = 64)
    String timezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 16)
    AuthProvider provider;

    @Column(name = "google_subject", length = 255)
    String googleSubject;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    UserEntity() {}
}
