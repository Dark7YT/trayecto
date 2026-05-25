package com.trayecto.sharing.interfaces.rest;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.sharing.application.command.AcceptAccessHandler;
import com.trayecto.sharing.application.command.InviteAccessHandler;
import com.trayecto.sharing.application.command.RejectAccessHandler;
import com.trayecto.sharing.application.command.RevokeAccessHandler;
import com.trayecto.sharing.application.query.ListReceivedInvitesHandler;
import com.trayecto.sharing.application.query.ListSentInvitesHandler;
import com.trayecto.sharing.application.query.ListSharedOwnersHandler;
import com.trayecto.sharing.application.query.ListSharedTripsHandler;
import com.trayecto.sharing.domain.AccessGrantId;
import com.trayecto.sharing.interfaces.rest.dto.AccessGrantResponse;
import com.trayecto.sharing.interfaces.rest.dto.InviteAccessRequest;
import com.trayecto.sharing.interfaces.rest.dto.OwnerSummaryResponse;
import com.trayecto.sharing.interfaces.rest.dto.SharedTripResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/sharing", produces = MediaType.APPLICATION_JSON_VALUE)
class SharingController {

    private final InviteAccessHandler inviteHandler;
    private final AcceptAccessHandler acceptHandler;
    private final RejectAccessHandler rejectHandler;
    private final RevokeAccessHandler revokeHandler;
    private final ListSentInvitesHandler listSent;
    private final ListReceivedInvitesHandler listReceived;
    private final ListSharedOwnersHandler listOwners;
    private final ListSharedTripsHandler listSharedTrips;

    SharingController(
        InviteAccessHandler inviteHandler,
        AcceptAccessHandler acceptHandler,
        RejectAccessHandler rejectHandler,
        RevokeAccessHandler revokeHandler,
        ListSentInvitesHandler listSent,
        ListReceivedInvitesHandler listReceived,
        ListSharedOwnersHandler listOwners,
        ListSharedTripsHandler listSharedTrips
    ) {
        this.inviteHandler = inviteHandler;
        this.acceptHandler = acceptHandler;
        this.rejectHandler = rejectHandler;
        this.revokeHandler = revokeHandler;
        this.listSent = listSent;
        this.listReceived = listReceived;
        this.listOwners = listOwners;
        this.listSharedTrips = listSharedTrips;
    }

    @PostMapping("/invite")
    ResponseEntity<Map<String, Object>> invite(
        @AuthenticationPrincipal UserId userId,
        @Valid @RequestBody InviteAccessRequest req
    ) {
        requireAuth(userId);
        var result = inviteHandler.handle(new InviteAccessHandler.Command(
            userId, req.granteeEmail(), req.recaptchaToken()
        ));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("id", result.id().value(), "status", "PENDING"));
    }

    @GetMapping("/sent")
    ResponseEntity<List<AccessGrantResponse>> sent(@AuthenticationPrincipal UserId userId) {
        requireAuth(userId);
        return ResponseEntity.ok(
            listSent.handle(new ListSentInvitesHandler.Query(userId))
                .stream().map(AccessGrantResponse::from).toList()
        );
    }

    @GetMapping("/received")
    ResponseEntity<List<AccessGrantResponse>> received(@AuthenticationPrincipal UserId userId) {
        requireAuth(userId);
        return ResponseEntity.ok(
            listReceived.handle(new ListReceivedInvitesHandler.Query(userId))
                .stream().map(AccessGrantResponse::from).toList()
        );
    }

    @PatchMapping("/{id}/accept")
    ResponseEntity<Void> accept(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID id
    ) {
        requireAuth(userId);
        acceptHandler.handle(new AcceptAccessHandler.Command(userId, AccessGrantId.of(id)));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reject")
    ResponseEntity<Void> reject(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID id
    ) {
        requireAuth(userId);
        rejectHandler.handle(new RejectAccessHandler.Command(userId, AccessGrantId.of(id)));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> revoke(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID id
    ) {
        requireAuth(userId);
        revokeHandler.handle(new RevokeAccessHandler.Command(userId, AccessGrantId.of(id)));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/owners")
    ResponseEntity<List<OwnerSummaryResponse>> owners(@AuthenticationPrincipal UserId userId) {
        requireAuth(userId);
        return ResponseEntity.ok(
            listOwners.handle(new ListSharedOwnersHandler.Query(userId))
                .stream().map(OwnerSummaryResponse::from).toList()
        );
    }

    @GetMapping("/trips/{ownerId}")
    ResponseEntity<List<SharedTripResponse>> sharedTrips(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID ownerId
    ) {
        requireAuth(userId);
        return ResponseEntity.ok(
            listSharedTrips.handle(new ListSharedTripsHandler.Query(userId, UserId.of(ownerId)))
                .stream().map(SharedTripResponse::from).toList()
        );
    }

    private static void requireAuth(UserId userId) {
        if (userId == null) throw new AccessDeniedException("Authentication required");
    }
}
