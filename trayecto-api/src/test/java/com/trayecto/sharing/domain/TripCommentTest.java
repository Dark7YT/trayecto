package com.trayecto.sharing.domain;

import com.trayecto.shared.kernel.TripId;
import com.trayecto.shared.kernel.UserId;
import com.trayecto.shared.kernel.exception.BusinessRuleViolation;
import com.trayecto.sharing.api.events.CommentAdded;
import com.trayecto.sharing.api.events.CommentDeleted;
import com.trayecto.sharing.api.events.CommentEdited;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripCommentTest {

    private static final TripId TRIP = TripId.newId();
    private static final UserId OWNER = UserId.newId();
    private static final UserId AUTHOR = UserId.newId();

    @Test
    void create_emitsCommentAdded_withPreview() {
        TripComment c = TripComment.create(TRIP, OWNER, AUTHOR, new CommentBody("Buen viaje"));
        assertThat(c.isDeleted()).isFalse();
        assertThat(c.pullDomainEvents()).hasSize(1).first().isInstanceOf(CommentAdded.class);
    }

    @Test
    void edit_byAuthor_setsEditedAt_andEmitsEvent() {
        TripComment c = TripComment.create(TRIP, OWNER, AUTHOR, new CommentBody("Original"));
        c.pullDomainEvents();

        c.edit(AUTHOR, new CommentBody("Editado"));

        assertThat(c.body().value()).isEqualTo("Editado");
        assertThat(c.editedAt()).isPresent();
        assertThat(c.pullDomainEvents()).hasSize(1).first().isInstanceOf(CommentEdited.class);
    }

    @Test
    void edit_byNonAuthor_rejected() {
        TripComment c = TripComment.create(TRIP, OWNER, AUTHOR, new CommentBody("Hi"));
        c.pullDomainEvents();
        UserId someoneElse = UserId.newId();

        assertThatThrownBy(() -> c.edit(someoneElse, new CommentBody("Hack")))
            .isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code()).isEqualTo("comment.not_author"));
    }

    @Test
    void softDelete_byAuthor_works() {
        TripComment c = TripComment.create(TRIP, OWNER, AUTHOR, new CommentBody("Hi"));
        c.pullDomainEvents();

        c.softDelete(AUTHOR);

        assertThat(c.isDeleted()).isTrue();
        assertThat(c.pullDomainEvents()).hasSize(1).first().isInstanceOf(CommentDeleted.class);
    }

    @Test
    void softDelete_byOwner_works() {
        // El owner del trip también puede borrar comentarios de otros (moderación).
        TripComment c = TripComment.create(TRIP, OWNER, AUTHOR, new CommentBody("Hi"));
        c.pullDomainEvents();

        c.softDelete(OWNER);

        assertThat(c.isDeleted()).isTrue();
    }

    @Test
    void softDelete_byStranger_rejected() {
        TripComment c = TripComment.create(TRIP, OWNER, AUTHOR, new CommentBody("Hi"));
        c.pullDomainEvents();
        UserId stranger = UserId.newId();

        assertThatThrownBy(() -> c.softDelete(stranger))
            .isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code()).isEqualTo("comment.not_authorized_delete"));
    }

    @Test
    void edit_afterDelete_rejected() {
        TripComment c = TripComment.create(TRIP, OWNER, AUTHOR, new CommentBody("Hi"));
        c.softDelete(AUTHOR);
        c.pullDomainEvents();

        assertThatThrownBy(() -> c.edit(AUTHOR, new CommentBody("X")))
            .isInstanceOf(BusinessRuleViolation.class)
            .satisfies(ex -> assertThat(((BusinessRuleViolation) ex).code()).isEqualTo("comment.deleted"));
    }
}
