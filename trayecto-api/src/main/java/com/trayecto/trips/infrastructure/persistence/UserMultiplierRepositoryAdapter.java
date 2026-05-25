package com.trayecto.trips.infrastructure.persistence;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.domain.MultiplierId;
import com.trayecto.trips.domain.UserMultiplier;
import com.trayecto.trips.domain.UserMultiplierRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class UserMultiplierRepositoryAdapter implements UserMultiplierRepository {

    private final UserMultiplierJpaRepository jpa;

    UserMultiplierRepositoryAdapter(UserMultiplierJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<UserMultiplier> findById(MultiplierId id) {
        return jpa.findById(id.value()).map(UserMultiplierMapper::toDomain);
    }

    @Override
    public List<UserMultiplier> findByUser(UserId userId) {
        return jpa.findByUserIdOrderByIsDefaultDescNameAsc(userId.value())
            .stream().map(UserMultiplierMapper::toDomain).toList();
    }

    @Override
    public Optional<UserMultiplier> findDefaultByUser(UserId userId) {
        return jpa.findByUserIdAndIsDefaultTrue(userId.value()).map(UserMultiplierMapper::toDomain);
    }

    @Override
    public long countByUser(UserId userId) {
        return jpa.countByUserId(userId.value());
    }

    @Override
    public boolean existsByUserAndName(UserId userId, String name) {
        return jpa.existsByUserIdAndName(userId.value(), name);
    }

    @Override
    public UserMultiplier save(UserMultiplier multiplier) {
        UserMultiplierEntity existing = jpa.findById(multiplier.id().value()).orElse(null);
        UserMultiplierEntity entity = UserMultiplierMapper.toEntity(multiplier, existing);
        return UserMultiplierMapper.toDomain(jpa.save(entity));
    }

    @Override
    public void delete(UserMultiplier multiplier) {
        jpa.deleteById(multiplier.id().value());
    }
}
