package com.trayecto.sharing.interfaces.rest;

import com.trayecto.iam.api.IamPublicApi;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.sharing.application.command.AddCommentHandler;
import com.trayecto.sharing.application.command.DeleteCommentHandler;
import com.trayecto.sharing.application.command.EditCommentHandler;
import com.trayecto.sharing.application.query.ListCommentsHandler;
import com.trayecto.sharing.domain.CommentId;
import com.trayecto.sharing.domain.TripComment;
import com.trayecto.sharing.interfaces.rest.dto.CommentRequest;
import com.trayecto.sharing.interfaces.rest.dto.CommentResponse;
import com.trayecto.shared.kernel.TripId;
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
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
class CommentController {

    private final AddCommentHandler addHandler;
    private final EditCommentHandler editHandler;
    private final DeleteCommentHandler deleteHandler;
    private final ListCommentsHandler listHandler;
    private final IamPublicApi iamApi;

    CommentController(
        AddCommentHandler addHandler,
        EditCommentHandler editHandler,
        DeleteCommentHandler deleteHandler,
        ListCommentsHandler listHandler,
        IamPublicApi iamApi
    ) {
        this.addHandler = addHandler;
        this.editHandler = editHandler;
        this.deleteHandler = deleteHandler;
        this.listHandler = listHandler;
        this.iamApi = iamApi;
    }

    @GetMapping("/api/v1/trips/{tripId}/comments")
    ResponseEntity<List<CommentResponse>> list(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID tripId
    ) {
        requireAuth(userId);
        List<TripComment> comments =
            listHandler.handle(new ListCommentsHandler.Query(userId, TripId.of(tripId)));
        // Resuelve displayName por author via iam.api. Si un autor fue eliminado,
        // fallback a "(usuario eliminado)" para no exponer UUID al frontend.
        List<CommentResponse> response = comments.stream()
            .map(c -> CommentResponse.from(c, resolveAuthorName(c.authorId())))
            .toList();
        return ResponseEntity.ok(response);
    }

    private String resolveAuthorName(UserId authorId) {
        return iamApi.findUserSnapshot(authorId)
            .map(snap -> snap.displayName())
            .orElse("(usuario eliminado)");
    }

    @PostMapping("/api/v1/trips/{tripId}/comments")
    ResponseEntity<Map<String, Object>> add(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID tripId,
        @Valid @RequestBody CommentRequest req
    ) {
        requireAuth(userId);
        var result = addHandler.handle(new AddCommentHandler.Command(userId, TripId.of(tripId), req.body()));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", result.commentId().value()));
    }

    @PatchMapping("/api/v1/comments/{id}")
    ResponseEntity<Void> edit(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID id,
        @Valid @RequestBody CommentRequest req
    ) {
        requireAuth(userId);
        editHandler.handle(new EditCommentHandler.Command(userId, CommentId.of(id), req.body()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/comments/{id}")
    ResponseEntity<Void> delete(
        @AuthenticationPrincipal UserId userId,
        @PathVariable UUID id
    ) {
        requireAuth(userId);
        deleteHandler.handle(new DeleteCommentHandler.Command(userId, CommentId.of(id)));
        return ResponseEntity.noContent().build();
    }

    private static void requireAuth(UserId userId) {
        if (userId == null) throw new AccessDeniedException("Authentication required");
    }
}
