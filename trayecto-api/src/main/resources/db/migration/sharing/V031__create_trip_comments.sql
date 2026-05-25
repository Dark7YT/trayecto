CREATE TABLE sharing_trip_comments (
    id              UUID                     PRIMARY KEY,
    trip_id         UUID                     NOT NULL,
    trip_owner_id   UUID                     NOT NULL,
    author_id       UUID                     NOT NULL,
    body            VARCHAR(2000)            NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    edited_at       TIMESTAMP WITH TIME ZONE,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX ix_sharing_comment_trip_created ON sharing_trip_comments (trip_id, created_at);
CREATE INDEX ix_sharing_comment_author        ON sharing_trip_comments (author_id);
CREATE INDEX ix_sharing_comment_active        ON sharing_trip_comments (trip_id) WHERE deleted_at IS NULL;
