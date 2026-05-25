package com.trayecto.iam.infrastructure.security;

import com.trayecto.iam.api.AccessTokenValidator;
import com.trayecto.shared.kernel.UserId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Extrae el JWT del header {@code Authorization: Bearer <token>}, lo valida y popula
 * el {@link SecurityContextHolder} con un Authentication cuyo principal es el {@link UserId}.
 * Si no hay token o es inválido, sigue la cadena — los endpoints protegidos rechazarán por sí solos.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain
    ) throws ServletException, IOException {
        Optional<String> bearer = extractBearer(request);
        if (bearer.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.validate(bearer.get()).ifPresent(principal -> {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal.userId(), null, List.of()
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }

    private static Optional<String> extractBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) return Optional.empty();
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
