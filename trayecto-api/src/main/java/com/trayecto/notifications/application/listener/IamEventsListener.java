package com.trayecto.notifications.application.listener;

import com.trayecto.iam.api.AuthProvider;
import com.trayecto.iam.api.events.PasswordResetRequested;
import com.trayecto.iam.api.events.RefreshTokenReuseDetected;
import com.trayecto.iam.api.events.UserEmailVerified;
import com.trayecto.iam.api.events.UserPasswordChanged;
import com.trayecto.iam.api.events.UserRegistered;
import com.trayecto.notifications.api.MailDispatchPort;
import com.trayecto.notifications.api.NotificationType;
import com.trayecto.notifications.application.NotificationDispatcher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Escucha eventos del módulo iam y dispara las notificaciones in-app + emails correspondientes.
 */
@Component
class IamEventsListener {

    private final NotificationDispatcher dispatcher;
    private final MailDispatchPort mail;

    IamEventsListener(NotificationDispatcher dispatcher, MailDispatchPort mail) {
        this.dispatcher = dispatcher;
        this.mail = mail;
    }

    @ApplicationModuleListener
    void onUserRegistered(UserRegistered event) {
        // 1) Email de verificación (solo si fue registro LOCAL, los OAuth ya están verificados)
        if (event.provider() == AuthProvider.LOCAL && event.emailVerificationTokenRaw() != null) {
            mail.sendEmailVerification(
                event.email(), event.displayName(), event.emailVerificationTokenRaw()
            );
        }
        // 2) Notificación in-app de bienvenida
        dispatcher.dispatch(
            event.userId(),
            NotificationType.USER_REGISTERED,
            "¡Bienvenido a Trayecto, " + event.displayName() + "!",
            Map.of(
                "provider", event.provider().name(),
                "registeredAt", event.registeredAt().toString()
            )
        );
    }

    @ApplicationModuleListener
    void onEmailVerified(UserEmailVerified event) {
        dispatcher.dispatch(
            event.userId(),
            NotificationType.EMAIL_VERIFIED,
            "Tu correo fue verificado",
            Map.of("email", event.email().value())
        );
    }

    @ApplicationModuleListener
    void onPasswordResetRequested(PasswordResetRequested event) {
        mail.sendPasswordReset(event.email(), event.resetTokenRaw());
        dispatcher.dispatch(
            event.userId(),
            NotificationType.PASSWORD_RESET_REQUESTED,
            "Solicitud de restablecimiento de contraseña",
            Map.of("email", event.email().value())
        );
    }

    @ApplicationModuleListener
    void onPasswordChanged(UserPasswordChanged event) {
        dispatcher.dispatch(
            event.userId(),
            NotificationType.PASSWORD_CHANGED,
            event.wasReset() ? "Contraseña restablecida" : "Contraseña actualizada",
            Map.of("wasReset", event.wasReset())
        );
    }

    @ApplicationModuleListener
    void onRefreshTokenReuse(RefreshTokenReuseDetected event) {
        dispatcher.dispatch(
            event.userId(),
            NotificationType.REFRESH_TOKEN_REUSE,
            "Alerta de seguridad: detectamos un intento sospechoso",
            Map.of(
                "familyId", event.familyId().toString(),
                "deviceFingerprint", event.deviceFingerprint() == null ? "" : event.deviceFingerprint()
            )
        );
    }
}
