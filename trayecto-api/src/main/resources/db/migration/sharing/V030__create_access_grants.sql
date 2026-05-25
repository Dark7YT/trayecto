CREATE TABLE sharing_access_grants (
    id                  UUID                     PRIMARY KEY,
    owner_id            UUID                     NOT NULL,
    grantee_email       VARCHAR(254)             NOT NULL,
    grantee_id          UUID,
    status              VARCHAR(16)              NOT NULL,
    invite_token_hash   VARCHAR(64)              NOT NULL UNIQUE,
    invited_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    responded_at        TIMESTAMP WITH TIME ZONE,
    revoked_at          TIMESTAMP WITH TIME ZONE,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_sharing_grant_status   CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'REVOKED')),
    CONSTRAINT ck_sharing_grant_not_self CHECK (grantee_id IS NULL OR grantee_id <> owner_id)
);

CREATE INDEX ix_sharing_grant_owner         ON sharing_access_grants (owner_id);
CREATE INDEX ix_sharing_grant_grantee_id    ON sharing_access_grants (grantee_id) WHERE grantee_id IS NOT NULL;
CREATE INDEX ix_sharing_grant_grantee_email ON sharing_access_grants (grantee_email);

-- Solo puede haber una invitación PENDING o ACCEPTED por par (owner, granteeEmail).
-- Después de REJECTED o REVOKED, el owner puede invitar de nuevo (nuevo registro).
CREATE UNIQUE INDEX ux_sharing_grant_active_invite
    ON sharing_access_grants (owner_id, LOWER(grantee_email))
    WHERE status IN ('PENDING', 'ACCEPTED');
