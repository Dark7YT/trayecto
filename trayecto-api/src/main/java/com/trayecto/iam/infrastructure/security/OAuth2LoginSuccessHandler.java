package com.trayecto.iam.infrastructure.security;

import com.trayecto.iam.domain.RefreshToken;
import com.trayecto.iam.domain.RefreshTokenRepository;
import com.trayecto.iam.domain.User;
import com.trayecto.iam.domain.UserRepository;
import com.trayecto.iam.interfaces.rest.RefreshTokenCookies;
import com.trayecto.shared.kernel.UserId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Al completar OAuth2 Google con éxito, emite access + refresh token igual que login local
 * y redirige al frontend con el access en el fragmento de la URL (para no exponerlo en
 * el query string que queda en el server log).
 * <p>
 * Marcado {@code @Lazy(false)} porque con {@code spring.main.lazy-initialization=true}
 * Spring Security puede construir el filter chain antes de inicializar este bean.
 */
@Component
@Lazy(false)
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final boolean secureCookies;

    @Value("${app.mail.app-url:http://localhost:3000}")
    private String appUrl;

    public OAuth2LoginSuccessHandler(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        JwtService jwtService,
        GoogleOAuth2UserService googleOAuth2UserService,
        Environment env
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.googleOAuth2UserService = googleOAuth2UserService;
        this.secureCookies = !env.matchesProfiles("dev");
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String userIdStr = GoogleOAuth2UserService.extractUserId(oauthUser);

        // Path normal: el GoogleOAuth2UserService enriqueció los attrs con trayecto_user_id.
        // Fallback: si por algún motivo Spring Security usó el DefaultOAuth2UserService
        // directamente y nuestros attrs enriched no llegaron, hacemos la upsert acá usando
        // los attrs crudos de Google. Doble red de seguridad — el usuario siempre logra
        // loguearse sin importar el camino que tomó Spring Security.
        User user;
        if (userIdStr != null && !userIdStr.isBlank()) {
            UserId userId = UserId.of(UUID.fromString(userIdStr));
            user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                    "User missing after OAuth login (id=" + userIdStr + ")"));
        } else {
            log.warn("OAuth user_id attribute missing — running upsert from raw Google attrs as fallback");
            try {
                user = googleOAuth2UserService.upsertFromGoogleAttributes(oauthUser.getAttributes());
            } catch (Exception e) {
                log.error("OAuth upsert fallback failed: {}", e.getMessage(), e);
                response.sendRedirect(appUrl + "/login?error=oauth_failed");
                return;
            }
        }

        String accessToken = jwtService.generateAccessToken(user.id(), user.email());
        String refreshRaw = jwtService.generateRefreshTokenRaw();
        Instant refreshExpiresAt = Instant.now().plusSeconds(jwtService.refreshTtlSeconds());
        RefreshToken token = RefreshToken.issueNewFamily(
            user.id(),
            jwtService.hashOpaqueToken(refreshRaw),
            refreshExpiresAt,
            "oauth-google"
        );
        refreshTokenRepository.save(token);

        // Cookie HttpOnly con el refresh raw.
        response.addHeader(HttpHeaders.SET_COOKIE,
            RefreshTokenCookies.buildSetCookie(refreshRaw, jwtService.refreshTtlSeconds(), secureCookies));

        // Redirigir al frontend con el access token. El frontend lo recoge del fragment
        // (no del query) para que no quede en logs/Referer.
        String redirectUrl = appUrl + "/oauth/callback#access_token="
            + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
            + "&expires_in=" + jwtService.accessTtlSeconds();
        log.info("OAuth Google login success for userId={}, redirecting", user.id().asString());
        response.sendRedirect(redirectUrl);
    }
}
