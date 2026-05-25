package com.trayecto.sharing.infrastructure.persistence;

import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.sharing.domain.AccessGrant;
import com.trayecto.sharing.domain.AccessGrantId;
import com.trayecto.sharing.domain.AccessGrantRepository;
import com.trayecto.sharing.domain.AccessGrantStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
class AccessGrantRepositoryAdapter implements AccessGrantRepository {

    private final AccessGrantJpaRepository jpa;

    AccessGrantRepositoryAdapter(AccessGrantJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<AccessGrant> findById(AccessGrantId id) {
        return jpa.findById(id.value()).map(AccessGrantMapper::toDomain);
    }

    @Override
    public Optional<AccessGrant> findByInviteTokenHash(String hash) {
        return jpa.findByInviteTokenHash(hash).map(AccessGrantMapper::toDomain);
    }

    @Override
    public List<AccessGrant> findSentByOwner(UserId ownerId) {
        return jpa.findByOwnerIdOrderByInvitedAtDesc(ownerId.value())
            .stream().map(AccessGrantMapper::toDomain).toList();
    }

    @Override
    public List<AccessGrant> findByGranteeEmail(Email granteeEmail) {
        return jpa.findByGranteeEmailOrderByInvitedAtDesc(granteeEmail.value().toLowerCase(Locale.ROOT))
            .stream().map(AccessGrantMapper::toDomain).toList();
    }

    @Override
    public List<AccessGrant> findActiveAccessFor(UserId granteeId) {
        return jpa.findActiveAccessForGrantee(granteeId.value())
            .stream().map(AccessGrantMapper::toDomain).toList();
    }

    @Override
    public List<UserId> findActiveOwnersFor(UserId granteeId) {
        return jpa.findActiveOwnerIdsFor(granteeId.value())
            .stream().map(UserId::of).toList();
    }

    @Override
    public boolean hasActiveAccess(UserId ownerId, UserId granteeId) {
        return jpa.existsByOwnerIdAndGranteeIdAndStatus(
            ownerId.value(), granteeId.value(), AccessGrantStatus.ACCEPTED
        );
    }

    @Override
    public boolean existsActiveInvite(UserId ownerId, Email granteeEmail) {
        return jpa.existsActiveInvite(ownerId.value(), granteeEmail.value());
    }

    @Override
    public AccessGrant save(AccessGrant grant) {
        AccessGrantEntity existing = jpa.findById(grant.id().value()).orElse(null);
        AccessGrantEntity entity = AccessGrantMapper.toEntity(grant, existing);
        return AccessGrantMapper.toDomain(jpa.save(entity));
    }
}
