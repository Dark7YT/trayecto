package com.trayecto.iam.api.events;

import com.trayecto.shared.kernel.UserId;

import java.time.Instant;

public record UserPasswordChanged(
    UserId userId,
    boolean wasReset,
    Instant changedAt
) {}
