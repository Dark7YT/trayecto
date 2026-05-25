package com.trayecto.iam.application.command;

import com.trayecto.iam.domain.DisplayName;
import com.trayecto.iam.domain.User;
import com.trayecto.iam.domain.UserRepository;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Locale;

@Component
public class UpdateProfileHandler {

    public record Command(UserId userId, String displayName, String locale, String timezone) {}

    private final UserRepository userRepository;

    public UpdateProfileHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void handle(Command command) {
        User user = userRepository.findById(command.userId())
            .orElseThrow(() -> new NotFoundException("user.not_found", "User not found"));
        user.updateProfile(
            new DisplayName(command.displayName()),
            Locale.forLanguageTag(command.locale()),
            ZoneId.of(command.timezone())
        );
        userRepository.save(user);
    }
}
