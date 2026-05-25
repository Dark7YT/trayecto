package com.trayecto.sharing.domain;

import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.sharing.api.events.AccessGrantAccepted;
import com.trayecto.sharing.api.events.AccessGrantInvited;
import com.trayecto.sharing.api.events.AccessGrantRejected;
import com.trayecto.sharing.api.events.AccessGrantRevoked;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Concesión de acceso para que un usuario (grantee) vea los viajes COMPLETED de otro (owner).
 * <p>
 * Flujo:
 * <pre>
 *   owner.invite(granteeEmail)            → PENDING (granteeId = null hasta que se registre/acepte)
 *      ↓                                       ↓
 *   grantee.accept()  → ACCEPTED          grantee.reject()  → REJECTED
 *      ↓                                       ↓
 *   owner.revoke()    → REVOKED           (terminal)
 * </pre>
 * Una vez REJECTED o REVOKED, el agregado es terminal — no se reabre. Para volver a
 * conceder acceso, se crea un nuevo AccessGrant.
 */
public final class AccessGrant {

    private final AccessGrantId id;
    private final UserId ownerId;
    private final Email granteeEmail;
    private UserId granteeId;            // null hasta accept
    private AccessGrantStatus status;
    private final String inviteTokenHash; // hash SHA-256 del token raw enviado por email
    private final Instant invitedAt;
    private Instant respondedAt;
    private Instant revokedAt;
    private Instant updatedAt;

    private final List<Object> domainEvents = new ArrayList<>();

    private AccessGrant(
        AccessGrantId id, UserId ownerId, Email granteeEmail, UserId granteeId,
        AccessGrantStatus status, String inviteTokenHash,
        Instant invitedAt, Instant respondedAt, Instant revokedAt, Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.granteeEmail = Objects.requireNonNull(granteeEmail);
        this.granteeId = granteeId;
        this.status = Objects.requireNonNull(status);
        this.inviteTokenHash = Objects.requireNonNull(inviteTokenHash);
        this.invitedAt = Objects.requireNonNull(invitedAt);
        this.respondedAt = respondedAt;
        this.revokedAt = revokedAt;
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static AccessGrant invite(
        UserId ownerId, Email granteeEmail, String inviteTokenRaw, String inviteTokenHash
    ) {
        Objects.requireNonNull(inviteTokenRaw, "inviteTokenRaw is required");
        Objects.requireNonNull(inviteTokenHash, "inviteTokenHash is required");
        Instant now = Instant.now();
        AccessGrant grant = new AccessGrant(
            AccessGrantId.newId(), ownerId, granteeEmail, null,
            AccessGrantStatus.PENDING, inviteTokenHash,
            now, null, null, now
        );
        grant.raise(new AccessGrantInvited(
            grant.id.value(), ownerId, granteeEmail, inviteTokenRaw, now
        ));
        return grant;
    }

    public static AccessGrant reconstitute(
        AccessGrantId id, UserId ownerId, Email granteeEmail, UserId granteeId,
        AccessGrantStatus status, String inviteTokenHash,
        Instant invitedAt, Instant respondedAt, Instant revokedAt, Instant updatedAt
    ) {
        return new AccessGrant(id, ownerId, granteeEmail, granteeId, status, inviteTokenHash,
            invitedAt, respondedAt, revokedAt, updatedAt);
    }

    public void accept(UserId acceptingUserId) {
        if (status == AccessGrantStatus.ACCEPTED) return; // idempotente
        if (status != AccessGrantStatus.PENDING) {
            throw new BusinessRuleViolation("access_grant.not_pending",
                "Only pending invitations can be accepted");
        }
        if (acceptingUserId.equals(ownerId)) {
            throw new BusinessRuleViolation("access_grant.owner_self_accept",
                "Owner cannot accept their own invitation");
        }
        Instant now = Instant.now();
        this.granteeId = acceptingUserId;
        this.status = AccessGrantStatus.ACCEPTED;
        this.respondedAt = now;
        this.updatedAt = now;
        raise(new AccessGrantAccepted(id.value(), ownerId, acceptingUserId, now));
    }

    public void reject(UserId rejectingUserId) {
        if (status == AccessGrantStatus.REJECTED) return;
        if (status != AccessGrantStatus.PENDING) {
            throw new BusinessRuleViolation("access_grant.not_pending",
                "Only pending invitations can be rejected");
        }
        Instant now = Instant.now();
        this.granteeId = rejectingUserId;
        this.status = AccessGrantStatus.REJECTED;
        this.respondedAt = now;
        this.updatedAt = now;
        raise(new AccessGrantRejected(id.value(), ownerId, rejectingUserId, now));
    }

    public void revoke() {
        if (status == AccessGrantStatus.REVOKED) return;
        if (status == AccessGrantStatus.REJECTED) {
            throw new BusinessRuleViolation("access_grant.cannot_revoke_rejected",
                "Cannot revoke a rejected invitation");
        }
        Instant now = Instant.now();
        UserId revokedGrantee = granteeId;
        this.status = AccessGrantStatus.REVOKED;
        this.revokedAt = now;
        this.updatedAt = now;
        raise(new AccessGrantRevoked(id.value(), ownerId, revokedGrantee, now));
    }

    private void raise(Object event) {
        domainEvents.add(event);
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public boolean grantsAccessTo(UserId userId) {
        return status == AccessGrantStatus.ACCEPTED
            && granteeId != null
            && granteeId.equals(userId);
    }

    // Getters
    public AccessGrantId id() { return id; }
    public UserId ownerId() { return ownerId; }
    public Email granteeEmail() { return granteeEmail; }
    public Optional<UserId> granteeId() { return Optional.ofNullable(granteeId); }
    public AccessGrantStatus status() { return status; }
    public String inviteTokenHash() { return inviteTokenHash; }
    public Instant invitedAt() { return invitedAt; }
    public Optional<Instant> respondedAt() { return Optional.ofNullable(respondedAt); }
    public Optional<Instant> revokedAt() { return Optional.ofNullable(revokedAt); }
    public Instant updatedAt() { return updatedAt; }
}
