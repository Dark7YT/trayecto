package com.trayecto.trips.application.query;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.domain.UserMultiplier;
import com.trayecto.trips.domain.UserMultiplierRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListMultipliersHandler {

    public record Query(UserId userId) {}

    private final UserMultiplierRepository repository;

    public ListMultipliersHandler(UserMultiplierRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<UserMultiplier> handle(Query query) {
        return repository.findByUser(query.userId());
    }
}
