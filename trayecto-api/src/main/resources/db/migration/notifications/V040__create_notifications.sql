CREATE TABLE notifications_notifications (
    id          UUID                     PRIMARY KEY,
    user_id     UUID                     NOT NULL,
    type        VARCHAR(40)              NOT NULL,
    title       VARCHAR(200)             NOT NULL,
    payload     JSONB                    NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at     TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_notif_type CHECK (type IN (
        'USER_REGISTERED', 'EMAIL_VERIFIED',
        'PASSWORD_RESET_REQUESTED', 'PASSWORD_CHANGED',
        'REFRESH_TOKEN_REUSE',
        'TRIP_COMPLETED', 'BUDGET_WARNING', 'BUDGET_EXCEEDED',
        'ACCESS_GRANT_INVITED', 'ACCESS_GRANT_ACCEPTED',
        'ACCESS_GRANT_REJECTED', 'ACCESS_GRANT_REVOKED',
        'COMMENT_ADDED'
    ))
);

CREATE INDEX ix_notif_user_created ON notifications_notifications (user_id, created_at DESC);

-- Índice partial muy barato para el contador "no leídas" del badge en la navbar.
CREATE INDEX ix_notif_user_unread
    ON notifications_notifications (user_id)
    WHERE read_at IS NULL;
