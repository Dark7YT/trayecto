package com.trayecto.iam.infrastructure.security;

import com.trayecto.iam.api.AccessTokenValidator;
import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.UuidV7;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Maneja firmas y verificaciones de JWT access tokens (HS256) y la generación/hash
 * de refresh tokens opacos (UUID v7).
 * <p>
 * Decisiones:
 * - Access tokens: HS256 stateless, payload mínimo (sub=userId, email, iat, exp).
 * - Refresh tokens: opacos. Raw = UUID v7 codificado base64url. Solo el hash SHA-256
 *   se persiste. Nunca son JWTs (evita revocación complicada).
 * - Reloj: {@code Instant.now()} — confiamos en NTP del servidor.
 */
@Service
public class JwtService implements AccessTokenValidator {

    private static final String CLAIM_EMAIL = "email";

    private final JwtProperties props;
    private final SecretKey signingKey;

    public JwtService(JwtProperties props) {
        this.props = props;
        byte[] keyBytes = decodeSecret(props.secret());
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "app.jwt.secret debe tener al menos 32 bytes (256 bits) tras decodificar base64. " +
                "Generalo con: [Convert]::ToBase64String((1..32 | ForEach-Object {Get-Random -Maximum 256}))"
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // ============ Access tokens ============

    public String generateAccessToken(UserId userId, Email email) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(props.accessTtlSeconds());
        return Jwts.builder()
            .subject(userId.asString())
            .claim(CLAIM_EMAIL, email.value())
            .issuedAt(java.util.Date.from(now))
            .expiration(java.util.Date.from(expiry))
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();
    }

    public Optional<AuthenticatedPrincipal> validateAccessToken(String token) {
        return validate(token);
    }

    @Override
    public Optional<AuthenticatedPrincipal> validate(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            UserId userId = UserId.of(UUID.fromString(claims.getSubject()));
            String emailValue = claims.get(CLAIM_EMAIL, String.class);
            Email email = emailValue == null ? null : Email.of(emailValue);
            return Optional.of(new AuthenticatedPrincipal(userId, email));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long accessTtlSeconds() {
        return props.accessTtlSeconds();
    }

    public long refreshTtlSeconds() {
        return props.refreshTtlSeconds();
    }

    // ============ Refresh tokens (opacos) ============

    /** Raw refresh token = UUID v7 codificado base64url (sin padding). */
    public String generateRefreshTokenRaw() {
        UUID uuid = UuidV7.randomUuid();
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) bytes[i] = (byte) (msb >>> (56 - i * 8));
        for (int i = 0; i < 8; i++) bytes[i + 8] = (byte) (lsb >>> (56 - i * 8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Hash SHA-256 hex. Lo que persistimos en BD. */
    public String hashOpaqueToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // ============ Helpers ============

    private static byte[] decodeSecret(String secret) {
        try {
            return Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            // Si no es Base64, usar bytes UTF-8 directos (solo para desarrollo).
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    // AuthenticatedPrincipal ahora vive en com.trayecto.iam.api.AccessTokenValidator
}
