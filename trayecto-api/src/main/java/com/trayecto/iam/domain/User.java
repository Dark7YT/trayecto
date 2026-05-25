package com.trayecto.iam.domain;

import com.trayecto.iam.api.AuthProvider;
import com.trayecto.iam.api.events.GoogleAccountLinked;
import com.trayecto.iam.api.events.UserEmailVerified;
import com.trayecto.iam.api.events.UserPasswordChanged;
import com.trayecto.iam.api.events.UserRegistered;
import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Agregado raíz del bounded context iam.
 * <p>
 * Es puro Java — no extiende clases de Spring ni se mapea con anotaciones JPA.
 * La persistencia ocurre en {@code iam.infrastructure.persistence.UserEntity} con MapStruct.
 * <p>
 * Las transiciones de estado producen eventos de dominio que el CommandHandler
 * recoge con {@link #pullDomainEvents()} y publica vía {@code ApplicationEventPublisher}
 * después de persistir.
 */
public final class User {

    private final UserId id;
    private Email email;
    private PasswordHash passwordHash; // null si solo Google
    private DisplayName displayName;
    private Locale locale;
    private ZoneId timezone;
    private UserStatus status;
    private AuthProvider provider;
    private String googleSubject; // null si solo LOCAL
    private final Instant createdAt;
    private Instant updatedAt;

    private final List<Object> domainEvents = new ArrayList<>();

    private User(
        UserId id,
        Email email,
        PasswordHash passwordHash,
        DisplayName displayName,
        Locale locale,
        ZoneId timezone,
        UserStatus status,
        AuthProvider provider,
        String googleSubject,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = passwordHash;
        this.displayName = Objects.requireNonNull(displayName);
        this.locale = Objects.requireNonNull(locale);
        this.timezone = Objects.requireNonNull(timezone);
        this.status = Objects.requireNonNull(status);
        this.provider = Objects.requireNonNull(provider);
        this.googleSubject = googleSubject;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    // ============ Factories ============

    public static User registerLocal(
        Email email,
        PasswordHash passwordHash,
        DisplayName displayName,
        Locale locale,
        ZoneId timezone,
        String emailVerificationTokenRaw
    ) {
        Objects.requireNonNull(passwordHash, "Local registration requires a password hash");
        Instant now = Instant.now();
        User user = new User(
            UserId.newId(),
            email,
            passwordHash,
            displayName,
            locale,
            timezone,
            UserStatus.PENDING_VERIFICATION,
            AuthProvider.LOCAL,
            null,
            now,
            now
        );
        user.raise(new UserRegistered(
            user.id, user.email, user.displayName.value(),
            AuthProvider.LOCAL, emailVerificationTokenRaw, now
        ));
        return user;
    }

    public static User registerWithGoogle(
        Email email,
        String googleSubject,
        DisplayName displayName,
        Locale locale,
        ZoneId timezone
    ) {
        Objects.requireNonNull(googleSubject, "Google registration requires a subject");
        if (googleSubject.isBlank()) {
            throw new BusinessRuleViolation("user.google_subject_blank",
                "Google subject must not be blank");
        }
        Instant now = Instant.now();
        User user = new User(
            UserId.newId(),
            email,
            null,
            displayName,
            locale,
            timezone,
            UserStatus.ACTIVE,  // Google pre-verifica el email
            AuthProvider.GOOGLE,
            googleSubject,
            now,
            now
        );
        user.raise(new UserRegistered(
            user.id, user.email, user.displayName.value(),
            AuthProvider.GOOGLE, null, now
        ));
        return user;
    }

    /** Reconstrucción desde persistencia (sin eventos). Usado por mappers. */
    public static User reconstitute(
        UserId id,
        Email email,
        PasswordHash passwordHash,
        DisplayName displayName,
        Locale locale,
        ZoneId timezone,
        UserStatus status,
        AuthProvider provider,
        String googleSubject,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new User(id, email, passwordHash, displayName, locale, timezone,
            status, provider, googleSubject, createdAt, updatedAt);
    }

    // ============ Comportamiento ============

    public void verifyEmail() {
        if (status == UserStatus.DEACTIVATED) {
            throw new BusinessRuleViolation("user.deactivated",
                "Cannot verify email of a deactivated account");
        }
        if (status == UserStatus.ACTIVE) {
            // Idempotente — no error, no evento duplicado
            return;
        }
        this.status = UserStatus.ACTIVE;
        this.updatedAt = Instant.now();
        raise(new UserEmailVerified(id, email, updatedAt));
    }

    public void changePassword(PasswordHash newHash, boolean wasReset) {
        if (status == UserStatus.DEACTIVATED) {
            throw new BusinessRuleViolation("user.deactivated",
                "Cannot change password of a deactivated account");
        }
        if (!provider.supportsLocalLogin()) {
            // Si era solo Google, ahora se vincula login local
            this.provider = AuthProvider.BOTH;
        }
        this.passwordHash = Objects.requireNonNull(newHash);
        this.updatedAt = Instant.now();
        raise(new UserPasswordChanged(id, wasReset, updatedAt));
    }

    public void linkGoogleAccount(String googleSubject) {
        Objects.requireNonNull(googleSubject);
        if (this.googleSubject != null && !this.googleSubject.equals(googleSubject)) {
            throw new BusinessRuleViolation("user.google_already_linked",
                "This account is already linked to a different Google identity");
        }
        if (status == UserStatus.DEACTIVATED) {
            throw new BusinessRuleViolation("user.deactivated",
                "Cannot link Google account to a deactivated user");
        }
        this.googleSubject = googleSubject;
        if (provider == AuthProvider.LOCAL) {
            this.provider = AuthProvider.BOTH;
        } else if (provider == AuthProvider.GOOGLE && status == UserStatus.PENDING_VERIFICATION) {
            // Google ya verificó el email
            this.status = UserStatus.ACTIVE;
        }
        this.updatedAt = Instant.now();
        raise(new GoogleAccountLinked(id, email, updatedAt));
    }

    public void updateProfile(DisplayName newDisplayName, Locale newLocale, ZoneId newTimezone) {
        if (status == UserStatus.DEACTIVATED) {
            throw new BusinessRuleViolation("user.deactivated",
                "Cannot update profile of a deactivated account");
        }
        this.displayName = Objects.requireNonNull(newDisplayName);
        this.locale = Objects.requireNonNull(newLocale);
        this.timezone = Objects.requireNonNull(newTimezone);
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        if (status == UserStatus.DEACTIVATED) {
            return; // idempotente
        }
        this.status = UserStatus.DEACTIVATED;
        this.updatedAt = Instant.now();
    }

    // ============ Queries ============

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean requiresEmailVerification() {
        return status == UserStatus.PENDING_VERIFICATION;
    }

    public boolean canLoginLocal() {
        return isActive() && provider.supportsLocalLogin() && passwordHash != null;
    }

    public boolean canLoginGoogle() {
        return isActive() && provider.supportsGoogleLogin() && googleSubject != null;
    }

    public Optional<PasswordHash> passwordHash() {
        return Optional.ofNullable(passwordHash);
    }

    public Optional<String> googleSubject() {
        return Optional.ofNullable(googleSubject);
    }

    // ============ Eventos de dominio ============

    private void raise(Object event) {
        domainEvents.add(event);
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    // ============ Getters (sin setters; mutación solo via métodos de negocio) ============

    public UserId id() { return id; }
    public Email email() { return email; }
    public DisplayName displayName() { return displayName; }
    public Locale locale() { return locale; }
    public ZoneId timezone() { return timezone; }
    public UserStatus status() { return status; }
    public AuthProvider provider() { return provider; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
