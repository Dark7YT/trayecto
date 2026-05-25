package com.trayecto.iam.application.command;

import com.trayecto.iam.domain.PasswordHash;
import com.trayecto.iam.domain.RefreshTokenRepository;
import com.trayecto.iam.domain.User;
import com.trayecto.iam.domain.UserRepository;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.shared.kernel.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class ChangePasswordHandler {

    public record Command(UserId userId, String currentRawPassword, String newRawPassword) {}

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    public ChangePasswordHandler(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        ApplicationEventPublisher events
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
    }

    @Transactional
    public void handle(Command command) {
        validatePasswordStrength(command.newRawPassword());

        User user = userRepository.findById(command.userId())
            .orElseThrow(() -> new NotFoundException("user.not_found", "User not found"));

        if (user.provider().supportsLocalLogin()) {
            // El usuario tenía password local: validar la actual.
            boolean ok = user.passwordHash()
                .map(h -> passwordEncoder.matches(command.currentRawPassword(), h.value()))
                .orElse(false);
            if (!ok) {
                throw new BusinessRuleViolation("auth.invalid_current_password",
                    "Current password is incorrect");
            }
        }
        // Si solo era Google, esto añade un password local (no se valida nada previo).

        user.changePassword(new PasswordHash(passwordEncoder.encode(command.newRawPassword())), false);
        userRepository.save(user);

        // Invalida sesiones existentes para forzar relogin con la nueva password.
        refreshTokenRepository.revokeAllForUser(user.id(), Instant.now());

        user.pullDomainEvents().forEach(events::publishEvent);
    }

    private static void validatePasswordStrength(String raw) {
        if (raw == null || raw.length() < 10 || raw.length() > 72) {
            throw new BusinessRuleViolation("user.password_invalid_length",
                "Password must be between 10 and 72 characters");
        }
        boolean hasLetter = raw.chars().anyMatch(Character::isLetter);
        boolean hasDigit = raw.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessRuleViolation("user.password_too_weak",
                "Password must contain at least one letter and one digit");
        }
    }
}
