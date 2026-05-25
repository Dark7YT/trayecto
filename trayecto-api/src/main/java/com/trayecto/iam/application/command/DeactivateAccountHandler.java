package com.trayecto.iam.application.command;

import com.trayecto.iam.domain.RefreshTokenRepository;
import com.trayecto.iam.domain.User;
import com.trayecto.iam.domain.UserRepository;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Marca la cuenta como DEACTIVATED y revoca todas las sesiones. No borra datos asociados
 * (viajes, comentarios) por integridad referencial.
 */
@Component
public class DeactivateAccountHandler {

    public record Command(UserId userId) {}

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public DeactivateAccountHandler(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public void handle(Command command) {
        User user = userRepository.findById(command.userId())
            .orElseThrow(() -> new NotFoundException("user.not_found", "User not found"));
        user.deactivate();
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.id(), Instant.now());
    }
}
