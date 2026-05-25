package com.trayecto.iam.application.command;

import com.trayecto.iam.domain.PasswordHash;
import com.trayecto.iam.domain.PasswordResetToken;
import com.trayecto.iam.domain.PasswordResetTokenRepository;
import com.trayecto.iam.domain.RefreshTokenRepository;
import com.trayecto.iam.domain.User;
import com.trayecto.iam.domain.UserRepository;
import com.trayecto.iam.infrastructure.security.JwtService;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.shared.kernel.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Aplica reset de password con un token. Tras reset, revoca todos los refresh tokens del
 * usuario (el atacante potencial pierde acceso, todas las sesiones activas deben relogin).
 */
@Component
public class ResetPasswordHandler {

    public record Command(String tokenRaw, String newRawPassword) {}

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher events;

    public ResetPasswordHandler(
        UserRepository userRepository,
        PasswordResetTokenRepository tokenRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        ApplicationEventPublisher events
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.events = events;
    }

    @Transactional
    public void handle(Command command) {
        if (command.tokenRaw() == null || command.tokenRaw().isBlank()) {
            throw new BusinessRuleViolation("password_reset.token_required", "Reset token is required");
        }
        validatePasswordStrength(command.newRawPassword());

        String hashed = jwtService.hashOpaqueToken(command.tokenRaw());
        PasswordResetToken token = tokenRepository.findByHashedToken(hashed)
            .orElseThrow(() -> new NotFoundException("password_reset.token_not_found",
                "Reset token not found or already used"));

        token.consume();

        User user = userRepository.findById(token.userId())
            .orElseThrow(() -> new NotFoundException("user.not_found", "User not found"));

        user.changePassword(new PasswordHash(passwordEncoder.encode(command.newRawPassword())), true);

        tokenRepository.save(token);
        userRepository.save(user);

        // Defensa en profundidad: invalidamos todas las sesiones del usuario al cambiar password.
        refreshTokenRepository.revokeAllForUser(user.id(), Instant.now());

        user.pullDomainEvents().forEach(events::publishEvent);
    }

    private static void validatePasswordStrength(String raw) {
        if (raw == null || raw.length() < 10) {
            throw new BusinessRuleViolation("user.password_too_short",
                "Password must be at least 10 characters");
        }
        if (raw.length() > 72) {
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
