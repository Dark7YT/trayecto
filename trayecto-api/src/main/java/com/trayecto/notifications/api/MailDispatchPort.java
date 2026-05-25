package com.trayecto.notifications.api;

import com.trayecto.shared.kernel.Email;

/**
 * Puerto público para envío de correos transaccionales. Implementado por
 * {@code notifications.infrastructure.mail.BrevoMailSender}. Cualquier módulo
 * (iam, sharing, trips) que necesite enviar email pasa por aquí.
 * <p>
 * Movido desde {@code iam.api} en Fase 4. La razón conceptual: enviar email
 * es parte del dominio de notificaciones, no de identity.
 */
public interface MailDispatchPort {

    void sendEmailVerification(Email to, String displayName, String tokenRaw);

    void sendPasswordReset(Email to, String tokenRaw);

    void sendShareInvitation(Email to, String ownerName, String tokenRaw);

    void sendShareAccepted(Email ownerEmail, String granteeName, String granteeEmail);

    void sendCommentNotification(Email to, String authorName, String preview, String tripId);
}
