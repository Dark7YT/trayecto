package com.trayecto.iam.application.command;

import com.trayecto.iam.api.AuthProvider;
import com.trayecto.iam.api.events.UserRegistered;
import com.trayecto.iam.domain.DisplayName;
import com.trayecto.iam.domain.EmailVerificationToken;
import com.trayecto.iam.domain.EmailVerificationTokenRepository;
import com.trayecto.iam.domain.PasswordHash;
import com.trayecto.iam.domain.User;
import com.trayecto.iam.domain.UserRepository;
import com.trayecto.iam.infrastructure.security.JwtService;
import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Component
public class RegisterUserHandler {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserHandler.class);


    /**
     * @param recaptchaToken puede ser null cuando el verificador está deshabilitado en dev.
     */
    public record Command(
        String email,
        String rawPassword,
        String displayName,
        String locale,
        String timezone,
        String recaptchaToken
    ) {}

    public record Result(UserId userId, Email email) {}

    private static final long EMAIL_VERIFICATION_TTL_HOURS = 24;

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher events;

    public RegisterUserHandler(
        UserRepository userRepository,
        EmailVerificationTokenRepository emailTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        ApplicationEventPublisher events
    ) {
        this.userRepository = userRepository;
        this.emailTokenRepository = emailTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.events = events;
    }

    @Transactional
    public Result handle(Command command) {
        Email email = Email.of(command.email());
        if (userRepository.existsByEmail(email)) {
            // No filtrar si el usuario ya existe — devolvemos un error genérico desde el controller
            // (HTTP 409) sin revelar enumeración. Aquí mantenemos el código por trazabilidad.
            throw new BusinessRuleViolation("user.email_already_registered",
                "An account with this email already exists");
        }

        DisplayName displayName = new DisplayName(command.displayName());
        Locale locale = Locale.forLanguageTag(command.locale());
        ZoneId timezone = ZoneId.of(command.timezone());

        validatePasswordStrength(command.rawPassword());
        PasswordHash passwordHash = new PasswordHash(passwordEncoder.encode(command.rawPassword()));

        String verificationTokenRaw = jwtService.generateRefreshTokenRaw();
        User user = User.registerLocal(email, passwordHash, displayName, locale, timezone, verificationTokenRaw);
        userRepository.save(user);

        EmailVerificationToken token = EmailVerificationToken.issue(
            user.id(),
            jwtService.hashOpaqueToken(verificationTokenRaw),
            Instant.now().plus(EMAIL_VERIFICATION_TTL_HOURS, ChronoUnit.HOURS)
        );
        emailTokenRepository.save(token);

        // Publicar el evento que se llevó dentro del agregado: notifications enviará el correo.
        for (Object event : user.pullDomainEvents()) {
            if (event instanceof UserRegistered ur) {
                // Sustituimos el tokenRaw con el real (el aggregate solo conoce un placeholder).
                events.publishEvent(new UserRegistered(
                    ur.userId(), ur.email(), ur.displayName(), AuthProvider.LOCAL,
                    verificationTokenRaw, ur.registeredAt()
                ));
            } else {
                events.publishEvent(event);
            }
        }

        // Dev helper: si el SMTP falla o aún no está configurado, el token raw queda en logs DEBUG
        // para poder verificar manualmente. NUNCA debe pasar a prod (logging level INFO en prod).
        log.debug("Issued email verification token for {} (raw={}, expiresIn=24h)",
            email.value(), verificationTokenRaw);

        return new Result(user.id(), email);
    }

    private static void validatePasswordStrength(String raw) {
        if (raw == null || raw.length() < 10) {
            throw new BusinessRuleViolation("user.password_too_short",
                "Password must be at least 10 characters");
        }
        if (raw.length() > 72) {
            // bcrypt limit
            throw new BusinessRuleViolation("user.password_too_long",
                "Password must be at most 72 characters");
        }
        boolean hasLetter = raw.chars().anyMatch(Character::isLetter);
        boolean hasDigit = raw.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessRuleViolation("user.password_too_weak",
                "Password must contain at least one letter and one digit");
        }
    }
}
