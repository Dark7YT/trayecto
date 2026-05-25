CREATE TABLE iam_email_verification_tokens (
    id            UUID         PRIMARY KEY,
    user_id       UUID         NOT NULL,
    hashed_token  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at   TIMESTAMP WITH TIME ZONE
);

CREATE INDEX ix_iam_evt_user_id    ON iam_email_verification_tokens (user_id);
CREATE INDEX ix_iam_evt_expires_at ON iam_email_verification_tokens (expires_at);
-- Solo tokens pendientes (no consumidos) — ayuda al flujo de invalidación previa.
CREATE INDEX ix_iam_evt_pending
    ON iam_email_verification_tokens (user_id)
    WHERE consumed_at IS NULL;
