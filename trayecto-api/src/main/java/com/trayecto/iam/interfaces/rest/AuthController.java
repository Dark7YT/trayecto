package com.trayecto.iam.interfaces.rest;

import com.trayecto.iam.application.command.ForgotPasswordHandler;
import com.trayecto.iam.application.command.LoginHandler;
import com.trayecto.iam.application.command.LogoutHandler;
import com.trayecto.iam.application.command.RefreshTokenHandler;
import com.trayecto.iam.application.command.RegisterUserHandler;
import com.trayecto.iam.application.command.ResetPasswordHandler;
import com.trayecto.iam.application.command.VerifyEmailHandler;
import com.trayecto.iam.interfaces.rest.dto.AuthResponse;
import com.trayecto.iam.interfaces.rest.dto.ForgotPasswordRequest;
import com.trayecto.iam.interfaces.rest.dto.LoginRequest;
import com.trayecto.iam.interfaces.rest.dto.RegisterRequest;
import com.trayecto.iam.interfaces.rest.dto.ResetPasswordRequest;
import com.trayecto.iam.interfaces.rest.dto.VerifyEmailRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
class AuthController {

    private final RegisterUserHandler registerUser;
    private final VerifyEmailHandler verifyEmail;
    private final LoginHandler login;
    private final RefreshTokenHandler refresh;
    private final LogoutHandler logout;
    private final ForgotPasswordHandler forgotPassword;
    private final ResetPasswordHandler resetPassword;
    private final boolean secureCookies;

    AuthController(
        RegisterUserHandler registerUser,
        VerifyEmailHandler verifyEmail,
        LoginHandler login,
        RefreshTokenHandler refresh,
        LogoutHandler logout,
        ForgotPasswordHandler forgotPassword,
        ResetPasswordHandler resetPassword,
        Environment env
    ) {
        this.registerUser = registerUser;
        this.verifyEmail = verifyEmail;
        this.login = login;
        this.refresh = refresh;
        this.logout = logout;
        this.forgotPassword = forgotPassword;
        this.resetPassword = resetPassword;
        // En dev (localhost http) Secure=true rompe la cookie. Solo activar en prod.
        this.secureCookies = !env.matchesProfiles("dev");
    }

    @PostMapping("/register")
    ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        var result = registerUser.handle(new RegisterUserHandler.Command(
            req.email(), req.password(), req.displayName(),
            req.locale(), req.timezone(), req.recaptchaToken()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "userId", result.userId().value(),
            "email", result.email().value(),
            "status", "PENDING_VERIFICATION",
            "message", "Registration successful. Please check your email to verify your account."
        ));
    }

    @PostMapping("/verify-email")
    ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        verifyEmail.handle(new VerifyEmailHandler.Command(req.token()));
        return ResponseEntity.ok(Map.of("status", "VERIFIED"));
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        var result = login.handle(new LoginHandler.Command(
            req.email(), req.password(), deviceFingerprint(http)
        ));
        String cookieHeader = RefreshTokenCookies.buildSetCookie(
            result.refreshTokenRaw(), result.refreshExpiresInSeconds(), secureCookies
        );
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookieHeader)
            .body(toAuthResponse(result));
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(HttpServletRequest http) {
        String rawToken = RefreshTokenCookies.readRefreshToken(http).orElse(null);
        var result = refresh.handle(new RefreshTokenHandler.Command(rawToken, deviceFingerprint(http)));
        String cookieHeader = RefreshTokenCookies.buildSetCookie(
            result.newRefreshTokenRaw(), result.refreshExpiresInSeconds(), secureCookies
        );
        AuthResponse body = new AuthResponse(
            result.newAccessToken(),
            result.accessExpiresInSeconds(),
            null  // /refresh no devuelve user info: el cliente ya lo tiene de /login
        );
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookieHeader)
            .body(body);
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest http) {
        String rawToken = RefreshTokenCookies.readRefreshToken(http).orElse(null);
        logout.handle(new LogoutHandler.Command(rawToken));
        String clearCookie = RefreshTokenCookies.buildClearCookie(secureCookies);
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, clearCookie)
            .build();
    }

    @PostMapping("/forgot-password")
    ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        forgotPassword.handle(new ForgotPasswordHandler.Command(req.email(), req.recaptchaToken()));
        // Siempre 202 — no revelamos si el email existe.
        return ResponseEntity.accepted().body(Map.of(
            "message", "If an account exists with that email, a reset link has been sent."
        ));
    }

    @PostMapping("/reset-password")
    ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        resetPassword.handle(new ResetPasswordHandler.Command(req.token(), req.newPassword()));
        return ResponseEntity.ok(Map.of("status", "PASSWORD_RESET"));
    }

    // ============ helpers ============

    private static AuthResponse toAuthResponse(LoginHandler.Result r) {
        return new AuthResponse(
            r.accessToken(),
            r.accessExpiresInSeconds(),
            new AuthResponse.UserInfo(
                r.user().id().value(),
                r.user().email().value(),
                r.user().displayName(),
                r.user().provider(),
                r.user().emailVerified()
            )
        );
    }

    /**
     * Huella opaca: hash de (user agent + IP). Estable para detectar cambios de cliente
     * pero no identifica individualmente al device. Solo informativo en logs/eventos.
     */
    private static String deviceFingerprint(HttpServletRequest http) {
        String ua = http.getHeader("User-Agent");
        String ip = http.getRemoteAddr();
        String raw = (ua == null ? "" : ua) + "|" + (ip == null ? "" : ip);
        return "fp-" + UUID.nameUUIDFromBytes(raw.getBytes()).toString();
    }
}
