package com.trayecto.trips.interfaces.rest;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.trips.application.command.CreateMultiplierHandler;
import com.trayecto.trips.application.command.DeleteMultiplierHandler;
import com.trayecto.trips.application.command.SetDefaultMultiplierHandler;
import com.trayecto.trips.application.command.UpdateMultiplierHandler;
import com.trayecto.trips.application.query.ListMultipliersHandler;
import com.trayecto.trips.domain.MultiplierId;
import com.trayecto.trips.interfaces.rest.dto.CreateMultiplierRequest;
import com.trayecto.trips.interfaces.rest.dto.MultiplierResponse;
import com.trayecto.trips.interfaces.rest.dto.UpdateMultiplierRequest;
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
@RequestMapping(value = "/api/v1/multipliers", produces = MediaType.APPLICATION_JSON_VALUE)
class MultiplierController {

    private final CreateMultiplierHandler createHandler;
    private final UpdateMultiplierHandler updateHandler;
    private final SetDefaultMultiplierHandler setDefaultHandler;
    private final DeleteMultiplierHandler deleteHandler;
    private final ListMultipliersHandler listHandler;

    MultiplierController(
        CreateMultiplierHandler createHandler,
        UpdateMultiplierHandler updateHandler,
        SetDefaultMultiplierHandler setDefaultHandler,
        DeleteMultiplierHandler deleteHandler,
        ListMultipliersHandler listHandler
    ) {
        this.createHandler = createHandler;
        this.updateHandler = updateHandler;
        this.setDefaultHandler = setDefaultHandler;
        this.deleteHandler = deleteHandler;
        this.listHandler = listHandler;
    }

    @GetMapping
    ResponseEntity<List<MultiplierResponse>> list(@AuthenticationPrincipal UserId userId) {
        requireAuth(userId);
        var items = listHandler.handle(new ListMultipliersHandler.Query(userId))
            .stream().map(MultiplierResponse::from).toList();
        return ResponseEntity.ok(items);
    }

    @PostMapping
    ResponseEntity<Map<String, Object>> create(
        @AuthenticationPrincipal UserId userId,
        @Valid @RequestBody CreateMultiplierRequest req
    ) {
        requireAuth(userId);
        var result = createHandler.handle(new CreateMultiplierHandler.Command(
            userId, req.name(), req.value(), req.asDefault()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", result.id().value()));
    }

    @PatchMapping("/{id}")
    ResponseEntity<Void> update(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateMultiplierRequest req
    ) {
        requireAuth(userId);
        updateHandler.handle(new UpdateMultiplierHandler.Command(
            userId, MultiplierId.of(id), req.name(), req.value()
        ));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/default")
    ResponseEntity<Void> setDefault(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID id
    ) {
        requireAuth(userId);
        setDefaultHandler.handle(new SetDefaultMultiplierHandler.Command(userId, MultiplierId.of(id)));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID id
    ) {
        requireAuth(userId);
        deleteHandler.handle(new DeleteMultiplierHandler.Command(userId, MultiplierId.of(id)));
        return ResponseEntity.noContent().build();
    }

    private static void requireAuth(UserId userId) {
        if (userId == null) throw new AccessDeniedException("Authentication required");
    }
}
