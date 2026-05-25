package com.trayecto.trips.domain;

import com.trayecto.shared.kernel.UserId;

import java.util.List;
import java.util.Optional;

public interface UserMultiplierRepository {

    Optional<UserMultiplier> findById(MultiplierId id);

    List<UserMultiplier> findByUser(UserId userId);

    Optional<UserMultiplier> findDefaultByUser(UserId userId);

    /** Contador para enforce de max 10. */
    long countByUser(UserId userId);

    boolean existsByUserAndName(UserId userId, String name);

    UserMultiplier save(UserMultiplier multiplier);

    void delete(UserMultiplier multiplier);
}
