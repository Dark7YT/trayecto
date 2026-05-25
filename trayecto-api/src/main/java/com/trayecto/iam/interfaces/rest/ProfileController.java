package com.trayecto.iam.interfaces.rest;

import com.trayecto.iam.application.command.ChangePasswordHandler;
import com.trayecto.iam.application.command.DeactivateAccountHandler;
import com.trayecto.iam.application.command.UpdateProfileHandler;
import com.trayecto.iam.application.query.GetProfileHandler;
import com.trayecto.iam.interfaces.rest.dto.ChangePasswordRequest;
import com.trayecto.iam.interfaces.rest.dto.ProfileResponse;
import com.trayecto.iam.interfaces.rest.dto.UpdateProfileRequest;
import com.trayecto.shared.kernel.UserId;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/profile", produces = MediaType.APPLICATION_JSON_VALUE)
class ProfileController {

    private final GetProfileHandler getProfile;
    private final UpdateProfileHandler updateProfile;
    private final ChangePasswordHandler changePassword;
    private final DeactivateAccountHandler deactivateAccount;

    ProfileController(
        GetProfileHandler getProfile,
        UpdateProfileHandler updateProfile,
        ChangePasswordHandler changePassword,
        DeactivateAccountHandler deactivateAccount
    ) {
        this.getProfile = getProfile;
        this.updateProfile = updateProfile;
        this.changePassword = changePassword;
        this.deactivateAccount = deactivateAccount;
    }

    @GetMapping
    ResponseEntity<ProfileResponse> me(@AuthenticationPrincipal UserId userId) {
        requireAuth(userId);
        var r = getProfile.handle(new GetProfileHandler.Query(userId));
        return ResponseEntity.ok(new ProfileResponse(
            r.userId().value(), r.email().value(), r.displayName(),
            r.locale(), r.timezone(), r.provider(),
            r.emailVerified(), r.createdAt(), r.updatedAt()
        ));
    }

    @PatchMapping
    ResponseEntity<Void> update(
        @AuthenticationPrincipal UserId userId,
        @Valid @RequestBody UpdateProfileRequest req
    ) {
        requireAuth(userId);
        updateProfile.handle(new UpdateProfileHandler.Command(
            userId, req.displayName(), req.locale(), req.timezone()
        ));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    ResponseEntity<Map<String, String>> changePassword(
        @AuthenticationPrincipal UserId userId,
        @Valid @RequestBody ChangePasswordRequest req
    ) {
        requireAuth(userId);
        changePassword.handle(new ChangePasswordHandler.Command(
            userId, req.currentPassword(), req.newPassword()
        ));
        return ResponseEntity.ok(Map.of(
            "status", "PASSWORD_CHANGED",
            "message", "Password updated. All sessions have been signed out."
        ));
    }

    @DeleteMapping
    ResponseEntity<Void> deactivate(@AuthenticationPrincipal UserId userId) {
        requireAuth(userId);
        deactivateAccount.handle(new DeactivateAccountHandler.Command(userId));
        return ResponseEntity.noContent().build();
    }

    private static void requireAuth(UserId userId) {
        if (userId == null) {
            throw new AccessDeniedException("Authentication required");
        }
    }
}
