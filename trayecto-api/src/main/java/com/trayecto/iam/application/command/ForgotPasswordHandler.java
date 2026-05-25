package com.trayecto.iam.application.command;

import com.trayecto.iam.api.events.PasswordResetRequested;
import com.trayecto.iam.domain.PasswordResetToken;
import com.trayecto.iam.domain.PasswordResetTokenRepository;
import com.trayecto.iam.domain.User;
import com.trayecto.iam.domain.UserRepository;
import com.trayecto.iam.infrastructure.security.JwtService;
import com.trayecto.shared.kernel.Email;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Solicita reset de password. Para evitar enumeración de usuarios, este handler NUNCA
 * lanza error si el email no existe — solo no emite el evento. La respuesta es siempre 202.
 */
@Component
public class ForgotPasswordHandler {

    public record Command(String email, String recaptchaToken) {}

    private static final long RESET_TTL_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JwtService jwtService;
    private final ApplicationEventPublisher events;

    public ForgotPasswordHandler(
        UserRepository userRepository,
        PasswordResetTokenRepository tokenRepository,
        JwtService jwtService,
        ApplicationEventPublisher events
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.jwtService = jwtService;
        this.events = events;
    }

    @Transactional
    public void handle(Command command) {
        Email email;
        try {
            email = Email.of(command.email());
        } catch (Exception bad) {
            // Email mal formado: respuesta silente. No revelamos el problema.
            return;
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !user.isActive() || !user.provider().supportsLocalLogin()) {
            // No revelamos si existe la cuenta ni si solo es Google.
            return;
        }

        // Invalidamos cualquier reset previo del mismo usuario.
        tokenRepository.invalidatePreviousFor(user.id(), Instant.now());

        String tokenRaw = jwtService.generateRefreshTokenRaw();
        PasswordResetToken token = PasswordResetToken.issue(
            user.id(),
            jwtService.hashOpaqueToken(tokenRaw),
            Instant.now().plus(RESET_TTL_HOURS, ChronoUnit.HOURS)
        );
        tokenRepository.save(token);

        events.publishEvent(new PasswordResetRequested(
            user.id(), user.email(), tokenRaw, Instant.now()
        ));
    }
}
