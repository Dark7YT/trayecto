package com.trayecto.iam.domain;

import com.trayecto.iam.api.AuthProvider;
import com.trayecto.iam.api.events.GoogleAccountLinked;
import com.trayecto.iam.api.events.UserEmailVerified;
import com.trayecto.iam.api.events.UserPasswordChanged;
import com.trayecto.iam.api.events.UserRegistered;
import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final Email EMAIL = Email.of("user@example.com");
    private static final PasswordHash HASH = new PasswordHash("$2b$12$0123456789012345678901abc");
    private static final DisplayName NAME = new DisplayName("Sebastian");
    private static final Locale LOCALE = Locale.forLanguageTag("es-PE");
    private static final ZoneId TZ = ZoneId.of("America/Lima");

    @Test
    void registerLocal_emitsUserRegistered_andStartsPendingVerification() {
        User user = User.registerLocal(EMAIL, HASH, NAME, LOCALE, TZ, "raw-token");

        assertThat(user.status()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(user.provider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(user.canLoginLocal()).isFalse(); // bloqueado hasta verificar email
        assertThat(user.requiresEmailVerification()).isTrue();

        List<Object> events = user.pullDomainEvents();
        assertThat(events).hasSize(1).first().isInstanceOf(UserRegistered.class);
        UserRegistered ev = (UserRegistered) events.getFirst();
        assertThat(ev.provider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(ev.emailVerificationTokenRaw()).isEqualTo("raw-token");
    }

    @Test
    void registerWithGoogle_skipsVerification_andLeavesActive() {
        User user = User.registerWithGoogle(EMAIL, "google-sub-123", NAME, LOCALE, TZ);

        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.provider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(user.canLoginGoogle()).isTrue();
        assertThat(user.canLoginLocal()).isFalse(); // no tiene password local
    }

    @Test
    void verifyEmail_promotesPendingToActive_andEmitsEvent() {
        User user = User.registerLocal(EMAIL, HASH, NAME, LOCALE, TZ, "raw");
        user.pullDomainEvents(); // descartar UserRegistered

        user.verifyEmail();

        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.canLoginLocal()).isTrue();
        assertThat(user.pullDomainEvents()).hasSize(1).first().isInstanceOf(UserEmailVerified.class);
    }

    @Test
    void verifyEmail_isIdempotent_whenAlreadyActive() {
        User user = User.registerWithGoogle(EMAIL, "sub", NAME, LOCALE, TZ);
        user.pullDomainEvents();

        user.verifyEmail(); // ya ACTIVE → no debe lanzar ni emitir evento

        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.pullDomainEvents()).isEmpty();
    }

    @Test
    void verifyEmail_rejectsDeactivated() {
        User user = User.registerLocal(EMAIL, HASH, NAME, LOCALE, TZ, "raw");
        user.deactivate();
        user.pullDomainEvents();

        assertThatThrownBy(user::verifyEmail)
            .isInstanceOf(BusinessRuleViolation.class)
            .hasMessageContaining("deactivated");
    }

    @Test
    void changePassword_promotesGoogleOnlyToBoth_andEmitsEvent() {
        User user = User.registerWithGoogle(EMAIL, "sub", NAME, LOCALE, TZ);
        user.pullDomainEvents();

        user.changePassword(HASH, false);

        assertThat(user.provider()).isEqualTo(AuthProvider.BOTH);
        assertThat(user.canLoginLocal()).isTrue();
        assertThat(user.canLoginGoogle()).isTrue();
        assertThat(user.pullDomainEvents()).hasSize(1).first().isInstanceOf(UserPasswordChanged.class);
    }

    @Test
    void linkGoogleAccount_promotesLocalToBoth_andEmitsEvent() {
        User user = User.registerLocal(EMAIL, HASH, NAME, LOCALE, TZ, "raw");
        user.verifyEmail();
        user.pullDomainEvents();

        user.linkGoogleAccount("google-sub-xyz");

        assertThat(user.provider()).isEqualTo(AuthProvider.BOTH);
        assertThat(user.googleSubject()).contains("google-sub-xyz");
        assertThat(user.pullDomainEvents()).hasSize(1).first().isInstanceOf(GoogleAccountLinked.class);
    }

    @Test
    void linkGoogleAccount_rejectsConflictingSubject() {
        User user = User.registerWithGoogle(EMAIL, "sub-A", NAME, LOCALE, TZ);
        user.pullDomainEvents();

        assertThatThrownBy(() -> user.linkGoogleAccount("sub-B"))
            .isInstanceOf(BusinessRuleViolation.class)
            .hasMessageContaining("already linked");
    }

    @Test
    void updateProfile_changesFields_withoutEvent() {
        User user = User.registerLocal(EMAIL, HASH, NAME, LOCALE, TZ, "raw");
        user.verifyEmail();
        user.pullDomainEvents();

        user.updateProfile(new DisplayName("Nuevo Nombre"), Locale.forLanguageTag("en-US"), ZoneId.of("UTC"));

        assertThat(user.displayName().value()).isEqualTo("Nuevo Nombre");
        assertThat(user.locale()).isEqualTo(Locale.forLanguageTag("en-US"));
        assertThat(user.timezone()).isEqualTo(ZoneId.of("UTC"));
        assertThat(user.pullDomainEvents()).isEmpty();
    }

    @Test
    void deactivate_setsStatusAndIsIdempotent() {
        User user = User.registerLocal(EMAIL, HASH, NAME, LOCALE, TZ, "raw");

        user.deactivate();
        user.deactivate(); // segunda llamada no debe lanzar

        assertThat(user.status()).isEqualTo(UserStatus.DEACTIVATED);
        assertThat(user.canLoginLocal()).isFalse();
    }
}
