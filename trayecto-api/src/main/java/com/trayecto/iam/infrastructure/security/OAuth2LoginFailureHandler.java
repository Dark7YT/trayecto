package com.trayecto.iam.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Maneja un fallo del login OAuth2 con Google.
 * <p>
 * Sin este handler, Spring Security cae a su página de login autogenerada en el
 * dominio del BACKEND ({@code trayecto-api.onrender.com/login?error}), que muestra
 * un feo "Invalid credentials". El usuario quedaba varado en una pantalla que no es
 * la app.
 * <p>
 * En su lugar:
 *  - Logueamos la causa real (clase + mensaje + error code OAuth) para diagnóstico.
 *  - Redirigimos al FRONTEND ({@code <APP_URL>/login?error=oauth}) para que la app
 *    muestre un mensaje en su propio diseño.
 */
@Component
@Lazy(false)
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    @Value("${app.mail.app-url:http://localhost:3000}")
    private String appUrl;

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        // Log detallado: en el segundo intento de Google esto nos dirá la causa raíz
        // exacta (qué excepción, qué OAuth2Error code) en los logs de Render.
        log.warn("OAuth2 login failed: {} - {}",
            exception.getClass().getSimpleName(), exception.getMessage(), exception);

        String base = appUrl != null && appUrl.endsWith("/")
            ? appUrl.substring(0, appUrl.length() - 1)
            : appUrl;
        response.sendRedirect(base + "/login?error=oauth");
    }
}
