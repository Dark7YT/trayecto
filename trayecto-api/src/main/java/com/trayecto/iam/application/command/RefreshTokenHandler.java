package com.trayecto.iam.application.command;

import com.trayecto.iam.api.events.RefreshTokenReuseDetected;
import com.trayecto.iam.domain.RefreshToken;
import com.trayecto.iam.domain.RefreshTokenRepository;
import com.trayecto.iam.domain.User;
import com.trayecto.iam.domain.UserRepository;
import com.trayecto.iam.infrastructure.security.JwtService;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.shared.kernel.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Rota un refresh token con detección de reuso. Si llega un token ya revocado,
 * revoca TODA la familia (asumimos robo) y emite un evento para notificar al usuario.
 */
@Component
public class RefreshTokenHandler {

    public record Command(String refreshTokenRaw, String deviceFingerprint) {}

    public record Result(
        String newAccessToken,
        String newRefreshTokenRaw,
        long accessExpiresInSeconds,
        long refreshExpiresInSeconds
    ) {}

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final ApplicationEventPublisher events;

    public RefreshTokenHandler(
        RefreshTokenRepository refreshTokenRepository,
        UserRepository userRepository,
        JwtService jwtService,
        ApplicationEventPublisher events
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.events = events;
    }

    @Transactional
    public Result handle(Command command) {
        if (command.refreshTokenRaw() == null || command.refreshTokenRaw().isBlank()) {
            throw new BusinessRuleViolation("auth.refresh_token_missing", "Refresh token is required");
        }
        String hashed = jwtService.hashOpaqueToken(command.refreshTokenRaw());
        RefreshToken existing = refreshTokenRepository.findByHashedToken(hashed)
            .orElseThrow(() -> new NotFoundException("auth.refresh_token_unknown",
                "Refresh token not recognised"));

        if (existing.isRevoked()) {
            // Reuso detectado: alguien intentó usar un token ya rotado. Asumimos robo.
            refreshTokenRepository.revokeFamily(existing.familyId(), Instant.now());
            events.publishEvent(new RefreshTokenReuseDetected(
                existing.userId(), existing.familyId(),
                command.deviceFingerprint(), Instant.now()
            ));
            throw new BusinessRuleViolation("auth.refresh_token_reuse",
                "Refresh token reuse detected; all sessions revoked");
        }

        if (existing.isExpired()) {
            throw new BusinessRuleViolation("auth.refresh_token_expired", "Refresh token has expired");
        }

        User user = userRepository.findById(existing.userId())
            .orElseThrow(() -> new NotFoundException("user.not_found", "User not found"));
        if (!user.isActive()) {
            throw new BusinessRuleViolation("auth.account_not_active", "Account is not active");
        }

        String newRaw = jwtService.generateRefreshTokenRaw();
        String newHashed = jwtService.hashOpaqueToken(newRaw);
        Instant newExpiresAt = Instant.now().plusSeconds(jwtService.refreshTtlSeconds());
        RefreshToken rotated = existing.rotateTo(newHashed, newExpiresAt, command.deviceFingerprint());

        refreshTokenRepository.save(existing); // persiste revokedAt + replacedByTokenId
        refreshTokenRepository.save(rotated);

        String accessToken = jwtService.generateAccessToken(user.id(), user.email());

        return new Result(
            accessToken, newRaw,
            jwtService.accessTtlSeconds(), jwtService.refreshTtlSeconds()
        );
    }
}
