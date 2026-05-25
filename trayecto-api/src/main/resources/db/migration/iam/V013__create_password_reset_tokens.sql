CREATE TABLE iam_password_reset_tokens (
    id            UUID         PRIMARY KEY,
    user_id       UUID         NOT NULL,
    hashed_token  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at   TIMESTAMP WITH TIME ZONE
);

CREATE INDEX ix_iam_prt_user_id    ON iam_password_reset_tokens (user_id);
CREATE INDEX ix_iam_prt_expires_at ON iam_password_reset_tokens (expires_at);
CREATE INDEX ix_iam_prt_pending
    ON iam_password_reset_tokens (user_id)
    WHERE consumed_at IS NULL;
