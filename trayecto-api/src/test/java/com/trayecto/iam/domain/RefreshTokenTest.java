package com.trayecto.iam.domain;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenTest {

    private static final UserId USER = UserId.newId();

    @Test
    void issueNewFamily_setsFreshFamily_andIsUsable() {
        RefreshToken token = RefreshToken.issueNewFamily(
            USER, "hashed-1", Instant.now().plusSeconds(3600), "device-1"
        );

        assertThat(token.userId()).isEqualTo(USER);
        assertThat(token.familyId()).isNotNull();
        assertThat(token.isUsable()).isTrue();
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.isExpired()).isFalse();
    }

    @Test
    void rotateTo_revokesOriginal_andPreservesFamily() {
        RefreshToken original = RefreshToken.issueNewFamily(
            USER, "hash-A", Instant.now().plusSeconds(3600), "device-1"
        );

        RefreshToken rotated = original.rotateTo(
            "hash-B", Instant.now().plusSeconds(3600), "device-1"
        );

        assertThat(original.isRevoked()).isTrue();
        assertThat(original.replacedByTokenId()).contains(rotated.id());
        assertThat(rotated.familyId()).isEqualTo(original.familyId()); // misma familia
        assertThat(rotated.isUsable()).isTrue();
        assertThat(rotated.hashedToken()).isEqualTo("hash-B");
    }

    @Test
    void rotateTo_rejectsAlreadyRevokedToken() {
        RefreshToken token = RefreshToken.issueNewFamily(
            USER, "hash", Instant.now().plusSeconds(3600), "d"
        );
        token.revoke();

        assertThatThrownBy(() -> token.rotateTo("hash-new", Instant.now().plusSeconds(3600), "d"))
            .isInstanceOf(BusinessRuleViolation.class)
            .hasMessageContaining("already revoked");
    }

    @Test
    void rotateTo_rejectsExpiredToken() {
        // expira en el futuro inmediato, "expirar" lo simulamos con un past expiresAt via reconstitute
        RefreshToken token = RefreshToken.reconstitute(
            TokenId.newId(), USER, "hash", java.util.UUID.randomUUID(),
            Instant.now().minus(1, ChronoUnit.SECONDS), // ya expirado
            Instant.now().minus(1, ChronoUnit.HOURS),
            "d", null, null
        );

        assertThat(token.isExpired()).isTrue();
        assertThatThrownBy(() -> token.rotateTo("hash-new", Instant.now().plusSeconds(3600), "d"))
            .isInstanceOf(BusinessRuleViolation.class)
            .hasMessageContaining("expired");
    }

    @Test
    void revoke_isIdempotent() {
        RefreshToken token = RefreshToken.issueNewFamily(
            USER, "hash", Instant.now().plusSeconds(3600), "d"
        );
        token.revoke();
        Instant firstRevocation = token.revokedAt().orElseThrow();

        token.revoke(); // segunda llamada

        assertThat(token.revokedAt()).contains(firstRevocation); // no se sobreescribe
    }

    @Test
    void issue_rejectsPastExpiration() {
        assertThatThrownBy(() ->
            RefreshToken.issue(USER, "hash", java.util.UUID.randomUUID(),
                Instant.now().minus(1, ChronoUnit.MINUTES), "d")
        ).isInstanceOf(BusinessRuleViolation.class);
    }
}
