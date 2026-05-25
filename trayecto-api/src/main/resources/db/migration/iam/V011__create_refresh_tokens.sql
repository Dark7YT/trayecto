CREATE TABLE iam_refresh_tokens (
    id                      UUID         PRIMARY KEY,
    user_id                 UUID         NOT NULL,
    hashed_token            VARCHAR(64)  NOT NULL UNIQUE,
    family_id               UUID         NOT NULL,
    expires_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    device_fingerprint      VARCHAR(128),
    revoked_at              TIMESTAMP WITH TIME ZONE,
    replaced_by_token_id    UUID
);

CREATE INDEX ix_iam_refresh_tokens_user_id     ON iam_refresh_tokens (user_id);
CREATE INDEX ix_iam_refresh_tokens_family_id   ON iam_refresh_tokens (family_id);
CREATE INDEX ix_iam_refresh_tokens_expires_at  ON iam_refresh_tokens (expires_at);
-- Partial index para acelerar la búsqueda de tokens vivos
CREATE INDEX ix_iam_refresh_tokens_active
    ON iam_refresh_tokens (user_id)
    WHERE revoked_at IS NULL;
