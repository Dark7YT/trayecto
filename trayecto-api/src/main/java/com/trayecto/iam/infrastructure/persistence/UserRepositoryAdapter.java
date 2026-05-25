package com.trayecto.iam.infrastructure.persistence;

import com.trayecto.iam.domain.User;
import com.trayecto.iam.domain.UserRepository;
import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpa.findById(id.value()).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByGoogleSubject(String googleSubject) {
        return jpa.findByGoogleSubject(googleSubject).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public User save(User user) {
        UserEntity existing = jpa.findById(user.id().value()).orElse(null);
        UserEntity entity = UserMapper.toEntity(user, existing);
        UserEntity saved = jpa.save(entity);
        return UserMapper.toDomain(saved);
    }

    @Override
    public void delete(User user) {
        jpa.deleteById(user.id().value());
    }
}
