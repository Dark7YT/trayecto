package com.trayecto.iam.interfaces.rest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Optional;

/**
 * Helper para manejar el refresh token como cookie HttpOnly.
 * <p>
 * Atributos:
 * - HttpOnly: JS del browser no la lee.
 * - Secure: solo HTTPS. En dev se desactiva para localhost.
 * - SameSite=Lax: protege CSRF sin romper navegación normal.
 * - Path=/: necesario para que el middleware del frontend ({@code proxy.ts}) detecte la
 *   presencia de sesión en TODAS las rutas. Restringir el path rompe el route guard.
 *   No expone más superficie: la cookie sigue siendo HttpOnly + SameSite=Lax.
 */
public final class RefreshTokenCookies {

    public static final String COOKIE_NAME = "trayecto_refresh";
    public static final String COOKIE_PATH = "/";

    private RefreshTokenCookies() {}

    public static String buildSetCookie(String rawToken, long ttlSeconds, boolean secure) {
        return ResponseCookie.from(COOKIE_NAME, rawToken)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path(COOKIE_PATH)
            .maxAge(Duration.ofSeconds(ttlSeconds))
            .build()
            .toString();
    }

    public static String buildClearCookie(boolean secure) {
        return ResponseCookie.from(COOKIE_NAME, "")
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path(COOKIE_PATH)
            .maxAge(0)
            .build()
            .toString();
    }

    public static Optional<String> readRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        for (Cookie c : cookies) {
            if (COOKIE_NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return Optional.of(c.getValue());
            }
        }
        return Optional.empty();
    }

    public static String setCookieHeader() {
        return HttpHeaders.SET_COOKIE;
    }
}
