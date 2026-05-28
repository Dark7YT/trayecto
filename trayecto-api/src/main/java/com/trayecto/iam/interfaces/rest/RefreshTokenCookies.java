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
 * - SameSite: depende del entorno
 *     · prod (secure=true)  → "None" (necesario para cross-domain Vercel↔Render).
 *                              Requiere Secure=true, lo cual se cumple en HTTPS.
 *     · dev  (secure=false) → "Lax" (frontend y backend en localhost, mismo site).
 * - Path=/: necesario para que el middleware del frontend ({@code proxy.ts}) detecte la
 *   presencia de sesión en TODAS las rutas. Restringir el path rompe el route guard.
 *   No expone más superficie: la cookie sigue siendo HttpOnly.
 * - Domain (opcional): solo se setea cuando la env var COOKIE_DOMAIN está definida.
 *   Útil cuando frontend y backend comparten dominio padre (ej. trayecto.app +
 *   api.trayecto.app → COOKIE_DOMAIN=.trayecto.app). Sin esa var, se omite y el
 *   browser asocia la cookie al host del backend, que sigue siendo válido cross-origin
 *   con SameSite=None.
 */
public final class RefreshTokenCookies {

    public static final String COOKIE_NAME = "trayecto_refresh";
    public static final String COOKIE_PATH = "/";

    private RefreshTokenCookies() {}

    public static String buildSetCookie(String rawToken, long ttlSeconds, boolean secure) {
        String sameSite = secure ? "None" : "Lax";
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(COOKIE_NAME, rawToken)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path(COOKIE_PATH)
            .maxAge(Duration.ofSeconds(ttlSeconds));
        applyCookieDomain(builder);
        return builder.build().toString();
    }

    public static String buildClearCookie(boolean secure) {
        String sameSite = secure ? "None" : "Lax";
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(COOKIE_NAME, "")
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path(COOKIE_PATH)
            .maxAge(0);
        applyCookieDomain(builder);
        return builder.build().toString();
    }

    private static void applyCookieDomain(ResponseCookie.ResponseCookieBuilder builder) {
        String domain = System.getenv("COOKIE_DOMAIN");
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain.trim());
        }
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
