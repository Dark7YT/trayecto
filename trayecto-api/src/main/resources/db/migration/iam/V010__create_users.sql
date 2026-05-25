CREATE TABLE iam_users (
    id              UUID         PRIMARY KEY,
    email           VARCHAR(254) NOT NULL UNIQUE,
    password_hash   VARCHAR(72),
    display_name    VARCHAR(50)  NOT NULL,
    locale          VARCHAR(16)  NOT NULL,
    timezone        VARCHAR(64)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    provider        VARCHAR(16)  NOT NULL,
    google_subject  VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_iam_users_status   CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'DEACTIVATED')),
    CONSTRAINT ck_iam_users_provider CHECK (provider IN ('LOCAL', 'GOOGLE', 'BOTH'))
);

-- Partial index: only non-null google_subject rows. Faster lookup during Google login.
CREATE UNIQUE INDEX ux_iam_users_google_subject
    ON iam_users (google_subject)
    WHERE google_subject IS NOT NULL;

CREATE INDEX ix_iam_users_status ON iam_users (status);
