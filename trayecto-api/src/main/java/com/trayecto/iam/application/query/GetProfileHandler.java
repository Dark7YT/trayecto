package com.trayecto.iam.application.query;

import com.trayecto.iam.api.AuthProvider;
import com.trayecto.iam.domain.User;
import com.trayecto.iam.domain.UserRepository;
import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class GetProfileHandler {

    public record Query(UserId userId) {}

    public record Result(
        UserId userId,
        Email email,
        String displayName,
        String locale,
        String timezone,
        AuthProvider provider,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt
    ) {}

    private final UserRepository userRepository;

    public GetProfileHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Result handle(Query query) {
        User user = userRepository.findById(query.userId())
            .orElseThrow(() -> new NotFoundException("user.not_found", "User not found"));
        return new Result(
            user.id(),
            user.email(),
            user.displayName().value(),
            user.locale().toLanguageTag(),
            user.timezone().getId(),
            user.provider(),
            user.isActive(),
            user.createdAt(),
            user.updatedAt()
        );
    }
}
