package com.trayecto.iam.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * Rate limiting in-memory con Bucket4j + Caffeine. Aplica solo a endpoints específicos
 * (login, register, forgot-password). El bucket se identifica por IP + path.
 * <p>
 * En memoria: si hay múltiples instancias de Render, cada una tiene su propio contador.
 * Es aceptable para frenar ataques básicos. Para algo más fuerte se necesita Redis,
 * pero para el plan free + load esperada no aplica.
 * <p>
 * Ordenado antes del JwtAuthenticationFilter para no consumir CPU autenticando un request
 * que va a rechazarse igual.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private record LimitRule(String method, String pathPrefix, int capacity, Duration refillPeriod) {
        String key() {
            return method + ":" + pathPrefix;
        }
    }

    private static final List<LimitRule> RULES = List.of(
        new LimitRule("POST", "/api/v1/auth/login",            5, Duration.ofMinutes(1)),
        new LimitRule("POST", "/api/v1/auth/register",         3, Duration.ofHours(1)),
        new LimitRule("POST", "/api/v1/auth/forgot-password",  3, Duration.ofHours(1)),
        new LimitRule("POST", "/api/v1/auth/reset-password",   5, Duration.ofMinutes(10))
    );

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofHours(2))
        .maximumSize(10_000)
        .build();

    private final JsonMapper jsonMapper;

    public RateLimitFilter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain
    ) throws ServletException, IOException {
        LimitRule rule = findRule(request);
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }
        String clientKey = clientFingerprint(request) + "|" + rule.key();
        Bucket bucket = buckets.get(clientKey, k -> newBucket(rule));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.addHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }
        writeTooManyRequests(response, probe, rule);
    }

    private static LimitRule findRule(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) return null;
        String path = request.getRequestURI();
        for (LimitRule rule : RULES) {
            if (path.equals(rule.pathPrefix) || path.startsWith(rule.pathPrefix + "/")) return rule;
        }
        return null;
    }

    private static Bucket newBucket(LimitRule rule) {
        Bandwidth limit = Bandwidth.builder()
            .capacity(rule.capacity())
            .refillIntervally(rule.capacity(), rule.refillPeriod())
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private static String clientFingerprint(HttpServletRequest request) {
        // X-Forwarded-For (cuando hay proxy) o IP directa.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(
        HttpServletResponse response, ConsumptionProbe probe, LimitRule rule
    ) throws IOException {
        long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too many requests. Retry in " + retryAfterSeconds + " seconds."
        );
        pd.setTitle("Too Many Requests");
        pd.setType(URI.create("/errors/auth.rate_limited"));
        pd.setProperty("code", "auth.rate_limited");
        pd.setProperty("retryAfterSeconds", retryAfterSeconds);
        pd.setProperty("limit", rule.capacity());
        pd.setProperty("windowSeconds", rule.refillPeriod().toSeconds());

        jsonMapper.writeValue(response.getOutputStream(), pd);
    }
}
