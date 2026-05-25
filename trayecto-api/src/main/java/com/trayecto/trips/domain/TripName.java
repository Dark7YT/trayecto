package com.trayecto.trips.domain;

import com.trayecto.shared.kernel.exception.BusinessRuleViolation;

public record TripName(String value) {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 100;

    public TripName {
        if (value == null) {
            throw new BusinessRuleViolation("trip.name_required", "Trip name is required");
        }
        value = value.trim();
        if (value.length() < MIN_LENGTH) {
            throw new BusinessRuleViolation("trip.name_too_short",
                "Trip name must not be empty");
        }
        if (value.length() > MAX_LENGTH) {
            throw new BusinessRuleViolation("trip.name_too_long",
                "Trip name must be at most " + MAX_LENGTH + " characters");
        }
    }
}
