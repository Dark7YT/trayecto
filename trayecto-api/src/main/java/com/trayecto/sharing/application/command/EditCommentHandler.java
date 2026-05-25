package com.trayecto.sharing.application.command;

import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.NotFoundException;
import com.trayecto.sharing.domain.CommentBody;
import com.trayecto.sharing.domain.CommentId;
import com.trayecto.sharing.domain.TripComment;
import com.trayecto.sharing.domain.TripCommentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EditCommentHandler {

    public record Command(UserId editorId, CommentId commentId, String newBody) {}

    private final TripCommentRepository repository;
    private final ApplicationEventPublisher events;

    public EditCommentHandler(TripCommentRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional
    public void handle(Command command) {
        TripComment comment = repository.findById(command.commentId())
            .orElseThrow(() -> new NotFoundException("comment.not_found", "Comment not found"));
        comment.edit(command.editorId(), new CommentBody(command.newBody()));
        repository.save(comment);
        comment.pullDomainEvents().forEach(events::publishEvent);
    }
}
