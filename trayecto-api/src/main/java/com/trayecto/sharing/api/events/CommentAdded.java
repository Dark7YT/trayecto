package com.trayecto.sharing.api.events;

import com.trayecto.shared.kernel.TripId;
import com.trayecto.shared.kernel.UserId;

import java.time.Instant;
import java.util.UUID;

/**
 * Se añadió un comentario a un viaje. {@code notifications} envía email a:
 * - Si el autor es el owner del trip → al/los granteeId que tengan acceso.
 * - Si el autor es un grantee → al owner.
 * <p>
 * {@code commentId} y {@code grantId} se exponen como {@link UUID} para no acoplar
 * consumidores externos al VO interno de sharing.
 */
public record CommentAdded(
    UUID commentId,
    TripId tripId,
    UserId tripOwnerId,
    UserId authorId,
    String preview,  // primeros 200 chars del body, para incluir en el email
    Instant createdAt
) {}
