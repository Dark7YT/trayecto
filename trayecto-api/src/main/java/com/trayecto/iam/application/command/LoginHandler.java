package com.trayecto.iam.application.command;

import com.trayecto.iam.api.AuthProvider;
import com.trayecto.iam.api.dto.UserSnapshot;
import com.trayecto.iam.domain.RefreshToken;
import com.trayecto.iam.domain.RefreshTokenRepository;
import com.trayecto.iam.domain.User;
import com.trayecto.iam.domain.UserRepository;
import com.trayecto.iam.domain.UserStatus;
import com.trayecto.iam.infrastructure.security.JwtService;
import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class LoginHandler {

    public record Command(String email, String rawPassword, String deviceFingerprint) {}

    /**
     * @param refreshTokenRaw el raw que va al cliente como cookie HttpOnly. Solo existe en este momento.
     */
    public record Result(
        String accessToken,
        String refreshTokenRaw,
        long accessExpiresInSeconds,
        long refreshExpiresInSeconds,
        UserSnapshot user
    ) {}

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginHandler(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public Result handle(Command command) {
        Email email = Email.of(command.email());
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessRuleViolation("auth.invalid_credentials",
                "Invalid email or password"));

        if (user.status() == UserStatus.DEACTIVATED) {
            throw new BusinessRuleViolation("auth.account_deactivated", "Account is deactivated");
        }
        if (user.status() == UserStatus.PENDING_VERIFICATION) {
            throw new BusinessRuleViolation("auth.email_not_verified",
                "Please verify your email before logging in");
        }
        if (!user.canLoginLocal()) {
            // El usuario solo tiene Google login configurado; sugerir el flujo correcto.
            throw new BusinessRuleViolation("auth.use_google_login",
                "This account uses Google sign-in; password login is not available");
        }

        boolean passwordOk = user.passwordHash()
            .map(h -> passwordEncoder.matches(command.rawPassword(), h.value()))
            .orElse(false);
        if (!passwordOk) {
            // Mismo código para no enumerar usuarios.
            throw new BusinessRuleViolation("auth.invalid_credentials", "Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user.id(), user.email());
        String refreshRaw = jwtService.generateRefreshTokenRaw();
        String refreshHashed = jwtService.hashOpaqueToken(refreshRaw);
        Instant refreshExpiresAt = Instant.now().plusSeconds(jwtService.refreshTtlSeconds());
        RefreshToken token = RefreshToken.issueNewFamily(
            user.id(), refreshHashed, refreshExpiresAt, command.deviceFingerprint()
        );
        refreshTokenRepository.save(token);

        UserSnapshot snapshot = new UserSnapshot(
            user.id(),
            user.email(),
            user.displayName().value(),
            user.provider() == null ? AuthProvider.LOCAL : user.provider(),
            user.isActive()
        );

        return new Result(
            accessToken, refreshRaw,
            jwtService.accessTtlSeconds(), jwtService.refreshTtlSeconds(),
            snapshot
        );
    }
}
