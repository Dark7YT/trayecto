CREATE TABLE trips_user_multipliers (
    id          UUID                     PRIMARY KEY,
    user_id     UUID                     NOT NULL,
    name        VARCHAR(50)              NOT NULL,
    value       NUMERIC(4,2)             NOT NULL,
    is_default  BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_trips_mult_value_positive CHECK (value > 0 AND value <= 99.99),
    CONSTRAINT ux_trips_mult_user_name UNIQUE (user_id, name)
);

CREATE INDEX ix_trips_mult_user ON trips_user_multipliers (user_id);

-- Solo un default por usuario (partial unique).
CREATE UNIQUE INDEX ux_trips_mult_user_default
    ON trips_user_multipliers (user_id)
    WHERE is_default = TRUE;
