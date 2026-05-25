package com.trayecto.sharing.interfaces.rest.dto;

import com.trayecto.iam.api.dto.UserSnapshot;

import java.util.UUID;

public record OwnerSummaryResponse(
    UUID userId,
    String email,
    String displayName
) {
    public static OwnerSummaryResponse from(UserSnapshot snapshot) {
        return new OwnerSummaryResponse(
            snapshot.id().value(),
            snapshot.email().value(),
            snapshot.displayName()
        );
    }
}
