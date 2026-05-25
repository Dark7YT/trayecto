package com.trayecto.sharing.domain;

import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.sharing.api.events.AccessGrantAccepted;
import com.trayecto.sharing.api.events.AccessGrantInvited;
import com.trayecto.sharing.api.events.AccessGrantRejected;
import com.trayecto.sharing.api.events.AccessGrantRevoked;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessGrantTest {

    private static final UserId OWNER = UserId.newId();
    private static final UserId GRANTEE = UserId.newId();
    private static final Email GRANTEE_EMAIL = Email.of("papa@example.com");

    @Test
    void invite_startsPending_andEmitsAccessGrantInvited() {
        AccessGrant grant = AccessGrant.invite(OWNER, GRANTEE_EMAIL, "raw", "hash");
        assertThat(grant.status()).isEqualTo(AccessGrantStatus.PENDING);
        assertThat(grant.granteeId()).isEmpty();
        assertThat(grant.pullDomainEvents()).hasSize(1).first().isInstanceOf(AccessGrantInvited.class);
    }

    @Test
    void accept_setsGranteeAndEmitsEvent() {
        AccessGrant grant = AccessGrant.invite(OWNER, GRANTEE_EMAIL, "raw", "hash");
        grant.pullDomainEvents();

        grant.accept(GRANTEE);

        assertThat(grant.status()).isEqualTo(AccessGrantStatus.ACCEPTED);
        assertThat(grant.granteeId()).contains(GRANTEE);
        assertThat(grant.grantsAccessTo(GRANTEE)).isTrue();
        assertThat(grant.pullDomainEvents()).hasSize(1).first().isInstanceOf(AccessGrantAccepted.class);
    }

    @Test
    void accept_isIdempotent() {
        AccessGrant grant = AccessGrant.invite(OWNER, GRANTEE_EMAIL, "raw", "hash");
        grant.accept(GRANTEE);
        grant.pullDomainEvents();
        grant.accept(GRANTEE);

        assertThat(grant.pullDomainEvents()).isEmpty();
    }

    @Test
    void accept_rejectsSelfAcceptanceByOwner() {
        AccessGrant grant = AccessGrant.invite(OWNER, GRANTEE_EMAIL, "raw", "hash");

        assertThatThrownBy(() -> grant.accept(OWNER))
            .isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code()).isEqualTo("access_grant.owner_self_accept"));
    }

    @Test
    void accept_rejectsAfterRevoked() {
        AccessGrant grant = AccessGrant.invite(OWNER, GRANTEE_EMAIL, "raw", "hash");
        grant.revoke();
        grant.pullDomainEvents();

        assertThatThrownBy(() -> grant.accept(GRANTEE))
            .isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code()).isEqualTo("access_grant.not_pending"));
    }

    @Test
    void reject_emitsEventAndIsTerminal() {
        AccessGrant grant = AccessGrant.invite(OWNER, GRANTEE_EMAIL, "raw", "hash");
        grant.pullDomainEvents();

        grant.reject(GRANTEE);

        assertThat(grant.status()).isEqualTo(AccessGrantStatus.REJECTED);
        assertThat(grant.pullDomainEvents()).hasSize(1).first().isInstanceOf(AccessGrantRejected.class);
        // No se puede revocar una rechazada
        assertThatThrownBy(grant::revoke)
            .isInstanceOf(BusinessRuleViolation.class);
    }

    @Test
    void revoke_fromAcceptedEmitsEventAndIsTerminal() {
        AccessGrant grant = AccessGrant.invite(OWNER, GRANTEE_EMAIL, "raw", "hash");
        grant.accept(GRANTEE);
        grant.pullDomainEvents();

        grant.revoke();

        assertThat(grant.status()).isEqualTo(AccessGrantStatus.REVOKED);
        assertThat(grant.grantsAccessTo(GRANTEE)).isFalse();
        assertThat(grant.pullDomainEvents()).hasSize(1).first().isInstanceOf(AccessGrantRevoked.class);
    }

    @Test
    void revoke_fromPendingEmitsEvent() {
        AccessGrant grant = AccessGrant.invite(OWNER, GRANTEE_EMAIL, "raw", "hash");
        grant.pullDomainEvents();

        grant.revoke();

        assertThat(grant.status()).isEqualTo(AccessGrantStatus.REVOKED);
        assertThat(grant.pullDomainEvents()).hasSize(1).first().isInstanceOf(AccessGrantRevoked.class);
    }
}
