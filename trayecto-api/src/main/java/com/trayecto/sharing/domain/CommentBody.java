package com.trayecto.sharing.domain;

import com.trayecto.shared.kernel.exception.BusinessRuleViolation;

public record CommentBody(String value) {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 2000;

    public CommentBody {
        if (value == null) {
            throw new BusinessRuleViolation("comment.body_required", "Comment body is required");
        }
        value = value.trim();
        if (value.length() < MIN_LENGTH) {
            throw new BusinessRuleViolation("comment.body_empty", "Comment body cannot be empty");
        }
        if (value.length() > MAX_LENGTH) {
            throw new BusinessRuleViolation("comment.body_too_long",
                "Comment must be at most " + MAX_LENGTH + " characters");
        }
    }
}
