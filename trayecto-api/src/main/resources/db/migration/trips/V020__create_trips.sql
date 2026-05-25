CREATE TABLE trips_trips (
    id                     UUID                     PRIMARY KEY,
    user_id                UUID                     NOT NULL,
    name                   VARCHAR(100)             NOT NULL,
    start_km               NUMERIC(8,1)             NOT NULL,
    end_km                 NUMERIC(8,1),
    multiplier_id          UUID,
    multiplier_rate        NUMERIC(4,2),
    total_cost_amount      NUMERIC(12,2),
    total_cost_currency    VARCHAR(3),
    status                 VARCHAR(16)              NOT NULL,
    start_photo_url        VARCHAR(512),
    end_photo_url          VARCHAR(512),
    started_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at           TIMESTAMP WITH TIME ZONE,
    cancelled_at           TIMESTAMP WITH TIME ZONE,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at             TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_trips_status CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_trips_end_after_start CHECK (end_km IS NULL OR end_km >= start_km),
    CONSTRAINT ck_trips_cost_currency_pair CHECK (
        (total_cost_amount IS NULL AND total_cost_currency IS NULL) OR
        (total_cost_amount IS NOT NULL AND total_cost_currency IS NOT NULL)
    )
);

CREATE INDEX ix_trips_user_started     ON trips_trips (user_id, started_at);
CREATE INDEX ix_trips_user_status      ON trips_trips (user_id, status);
CREATE INDEX ix_trips_completed_at     ON trips_trips (completed_at) WHERE status = 'COMPLETED';
CREATE INDEX ix_trips_user_not_deleted ON trips_trips (user_id) WHERE deleted_at IS NULL;
