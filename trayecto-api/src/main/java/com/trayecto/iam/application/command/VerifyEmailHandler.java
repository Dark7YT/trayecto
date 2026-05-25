package com.trayecto.iam.application.command;

import com.trayecto.iam.domain.EmailVerificationToken;
import com.trayecto.iam.domain.EmailVerificationTokenRepository;
import com.trayecto.iam.domain.User;
import com.trayecto.iam.domain.UserRepository;
import com.trayecto.iam.infrastructure.security.JwtService;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.shared.kernel.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class VerifyEmailHandler {

    public record Command(String tokenRaw) {}

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final JwtService jwtService;
    private final ApplicationEventPublisher events;

    public VerifyEmailHandler(
        UserRepository userRepository,
        EmailVerificationTokenRepository tokenRepository,
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
        if (command.tokenRaw() == null || command.tokenRaw().isBlank()) {
            throw new BusinessRuleViolation("email_verification.token_required",
                "Verification token is required");
        }
        String hashed = jwtService.hashOpaqueToken(command.tokenRaw());
        EmailVerificationToken token = tokenRepository.findByHashedToken(hashed)
            .orElseThrow(() -> new NotFoundException("email_verification.token_not_found",
                "Verification token not found or already used"));

        token.consume(); // valida no expirado/no consumido

        User user = userRepository.findById(token.userId())
            .orElseThrow(() -> new NotFoundException("user.not_found",
                "User not found for verification token"));

        user.verifyEmail();

        tokenRepository.save(token);
        userRepository.save(user);

        user.pullDomainEvents().forEach(events::publishEvent);
    }
}
