package com.trayecto.iam.application;

import com.trayecto.iam.api.IamPublicApi;
import com.trayecto.iam.api.dto.UserSnapshot;
import com.trayecto.iam.domain.User;
import com.trayecto.iam.domain.UserRepository;
import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implementación de {@link IamPublicApi} expuesta a otros bounded contexts vía DI.
 * Solo Spring Modulith permite que esta clase sea visible: vive en {@code iam.application}
 * (paquete interno) pero implementa una interfaz declarada en {@code iam.api} (public).
 */
@Component
class IamPublicApiAdapter implements IamPublicApi {

    private final UserRepository userRepository;

    IamPublicApiAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserSnapshot> findUserSnapshot(UserId userId) {
        return userRepository.findById(userId).map(IamPublicApiAdapter::toSnapshot);
    }

    @Override
    public Optional<UserSnapshot> findUserSnapshotByEmail(Email email) {
        return userRepository.findByEmail(email).map(IamPublicApiAdapter::toSnapshot);
    }

    @Override
    public boolean userIsActive(UserId userId) {
        return userRepository.findById(userId).map(User::isActive).orElse(false);
    }

    private static UserSnapshot toSnapshot(User user) {
        return new UserSnapshot(
            user.id(),
            user.email(),
            user.displayName().value(),
            user.provider(),
            user.isActive()
        );
    }
}
