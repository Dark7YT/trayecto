package com.trayecto.iam.domain;

import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;

import java.util.Optional;

/**
 * Puerto del dominio para persistencia de {@link User}. La implementación vive en
 * {@code iam.infrastructure.persistence}.
 */
public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    Optional<User> findByGoogleSubject(String googleSubject);

    boolean existsByEmail(Email email);

    User save(User user);

    void delete(User user);
}
