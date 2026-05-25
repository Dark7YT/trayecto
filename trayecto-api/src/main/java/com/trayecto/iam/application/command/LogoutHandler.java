package com.trayecto.iam.application.command;

import com.trayecto.iam.domain.RefreshToken;
import com.trayecto.iam.domain.RefreshTokenRepository;
import com.trayecto.iam.infrastructure.security.JwtService;
import com.trayecto.shared.kernel.UserId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Logout: revoca el refresh token actual. NO revoca la familia entera — el usuario podría
 * estar logueado en otros dispositivos. Para "logout de todos los dispositivos" usar el
 * comando dedicado en {@code LogoutAllHandler} (cuando se implemente).
 */
@Component
public class LogoutHandler {

    public record Command(String refreshTokenRaw) {}

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public LogoutHandler(RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public void handle(Command command) {
        if (command.refreshTokenRaw() == null || command.refreshTokenRaw().isBlank()) {
            return; // logout silencioso si no hay token (idempotente)
        }
        String hashed = jwtService.hashOpaqueToken(command.refreshTokenRaw());
        refreshTokenRepository.findByHashedToken(hashed).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
        });
    }

    /** Variante para revocar TODA la sesión del usuario (logout all devices). */
    @Transactional
    public void handleLogoutAll(UserId userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }
}
