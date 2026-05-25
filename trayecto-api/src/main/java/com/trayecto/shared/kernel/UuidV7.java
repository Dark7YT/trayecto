package com.trayecto.shared.kernel;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * UUID v7 generator (time-ordered, RFC 9562).
 * <p>
 * Layout (128 bits):
 * <pre>
 *  48 bits unix_ts_ms | 4 bits ver=7 | 12 bits rand_a | 2 bits var=10 | 62 bits rand_b
 * </pre>
 * Time-ordered IDs play well with BTree indexes in Postgres and remain globally unique.
 * <p>
 * This is intentionally a small inline implementation to avoid pulling in an extra
 * dependency for one feature. Swap to {@code com.github.f4b6a3:uuid-creator} if more
 * UUID flavors are needed in the future.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {}

    public static UUID randomUuid() {
        long timestamp = System.currentTimeMillis();
        byte[] randomBytes = new byte[10];
        RANDOM.nextBytes(randomBytes);

        long msb = (timestamp & 0xFFFF_FFFF_FFFFL) << 16;
        msb |= 0x7000L;
        msb |= ((long) (randomBytes[0] & 0x0F) << 8) | (randomBytes[1] & 0xFFL);

        long lsb = 0L;
        lsb |= ((long) (randomBytes[2] & 0x3F) | 0x80L) << 56;
        lsb |= ((long) (randomBytes[3] & 0xFF)) << 48;
        lsb |= ((long) (randomBytes[4] & 0xFF)) << 40;
        lsb |= ((long) (randomBytes[5] & 0xFF)) << 32;
        lsb |= ((long) (randomBytes[6] & 0xFF)) << 24;
        lsb |= ((long) (randomBytes[7] & 0xFF)) << 16;
        lsb |= ((long) (randomBytes[8] & 0xFF)) << 8;
        lsb |= randomBytes[9] & 0xFFL;

        return new UUID(msb, lsb);
    }
}
