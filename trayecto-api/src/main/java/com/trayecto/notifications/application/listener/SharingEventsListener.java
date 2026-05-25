package com.trayecto.notifications.application.listener;

import com.trayecto.iam.api.IamPublicApi;
import com.trayecto.notifications.api.MailDispatchPort;
import com.trayecto.notifications.api.NotificationType;
import com.trayecto.notifications.application.NotificationDispatcher;
import com.trayecto.sharing.api.events.AccessGrantAccepted;
import com.trayecto.sharing.api.events.AccessGrantInvited;
import com.trayecto.sharing.api.events.AccessGrantRejected;
import com.trayecto.sharing.api.events.AccessGrantRevoked;
import com.trayecto.sharing.api.events.CommentAdded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class SharingEventsListener {

    private static final Logger log = LoggerFactory.getLogger(SharingEventsListener.class);

    private final NotificationDispatcher dispatcher;
    private final MailDispatchPort mail;
    private final IamPublicApi iam;

    SharingEventsListener(
        NotificationDispatcher dispatcher,
        MailDispatchPort mail,
        IamPublicApi iam
    ) {
        this.dispatcher = dispatcher;
        this.mail = mail;
        this.iam = iam;
    }

    @ApplicationModuleListener
    void onAccessGrantInvited(AccessGrantInvited event) {
        // Email a quien invitamos
        String ownerName = iam.findUserSnapshot(event.ownerId())
            .map(s -> s.displayName())
            .orElse("alguien");
        mail.sendShareInvitation(event.granteeEmail(), ownerName, event.inviteTokenRaw());

        // Si el grantee YA es usuario registrado, notifica in-app también
        iam.findUserSnapshotByEmail(event.granteeEmail()).ifPresent(snapshot ->
            dispatcher.dispatch(
                snapshot.id(),
                NotificationType.ACCESS_GRANT_INVITED,
                ownerName + " quiere compartir sus viajes contigo",
                Map.of(
                    "grantId", event.grantId().toString(),
                    "ownerId", event.ownerId().asString(),
                    "ownerName", ownerName
                )
            )
        );
    }

    @ApplicationModuleListener
    void onAccessGrantAccepted(AccessGrantAccepted event) {
        var ownerSnapshot = iam.findUserSnapshot(event.ownerId()).orElse(null);
        var granteeSnapshot = iam.findUserSnapshot(event.granteeId()).orElse(null);
        if (ownerSnapshot == null || granteeSnapshot == null) {
            log.warn("Cannot dispatch ACCESS_GRANT_ACCEPTED: owner or grantee not resolved");
            return;
        }
        mail.sendShareAccepted(
            ownerSnapshot.email(), granteeSnapshot.displayName(), granteeSnapshot.email().value()
        );
        dispatcher.dispatch(
            event.ownerId(),
            NotificationType.ACCESS_GRANT_ACCEPTED,
            granteeSnapshot.displayName() + " aceptó tu invitación",
            Map.of(
                "grantId", event.grantId().toString(),
                "granteeId", event.granteeId().asString(),
                "granteeName", granteeSnapshot.displayName()
            )
        );
    }

    @ApplicationModuleListener
    void onAccessGrantRejected(AccessGrantRejected event) {
        dispatcher.dispatch(
            event.ownerId(),
            NotificationType.ACCESS_GRANT_REJECTED,
            "Una invitación fue rechazada",
            Map.of(
                "grantId", event.grantId().toString(),
                "granteeId", event.granteeId().asString()
            )
        );
    }

    @ApplicationModuleListener
    void onAccessGrantRevoked(AccessGrantRevoked event) {
        if (event.granteeId() == null) return; // nunca aceptado, nadie a quien notificar
        dispatcher.dispatch(
            event.granteeId(),
            NotificationType.ACCESS_GRANT_REVOKED,
            "Te revocaron el acceso a unos viajes",
            Map.of(
                "grantId", event.grantId().toString(),
                "ownerId", event.ownerId().asString()
            )
        );
    }

    @ApplicationModuleListener
    void onCommentAdded(CommentAdded event) {
        // Notificar al participante opuesto
        boolean authorIsOwner = event.authorId().equals(event.tripOwnerId());
        var authorSnapshot = iam.findUserSnapshot(event.authorId()).orElse(null);
        if (authorSnapshot == null) return;

        String title = authorSnapshot.displayName() + " comentó en un viaje";

        if (!authorIsOwner) {
            // Grantee comentó → notif y email al owner
            var ownerSnapshot = iam.findUserSnapshot(event.tripOwnerId()).orElse(null);
            if (ownerSnapshot != null) {
                mail.sendCommentNotification(
                    ownerSnapshot.email(),
                    authorSnapshot.displayName(),
                    event.preview(),
                    event.tripId().asString()
                );
                dispatcher.dispatch(
                    event.tripOwnerId(),
                    NotificationType.COMMENT_ADDED,
                    title,
                    Map.of(
                        "tripId", event.tripId().asString(),
                        "commentId", event.commentId().toString(),
                        "authorId", event.authorId().asString(),
                        "authorName", authorSnapshot.displayName(),
                        "preview", event.preview()
                    )
                );
            }
        }
        // Owner comentando → notificar a grantees en Fase 4 sería bulk. En MVP solo loguear.
    }
}
