package com.trayecto.iam.api.events;

import com.trayecto.shared.kernel.Email;
import com.trayecto.shared.kernel.UserId;

import java.time.Instant;

public record UserEmailVerified(
    UserId userId,
    Email email,
    Instant verifiedAt
) {}
