package com.trayecto.shared.kernel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Utilidades para tokens opacos (refresh, email verification, password reset, invite).
 * <p>
 * - {@link #randomRaw()} genera un UUID v7 base64url sin padding (~22 chars).
 * - {@link #sha256Hex(String)} produce el hash que se persiste en BD (el raw nunca se guarda).
 * <p>
 * En shared porque la usan tanto {@code iam} (JWT/refresh/email verification/reset)
 * como {@code sharing} (invite tokens).
 */
public final class OpaqueTokens {

    private OpaqueTokens() {}

    public static String randomRaw() {
        UUID uuid = UuidV7.randomUuid();
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) bytes[i] = (byte) (msb >>> (56 - i * 8));
        for (int i = 0; i < 8; i++) bytes[i + 8] = (byte) (lsb >>> (56 - i * 8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
