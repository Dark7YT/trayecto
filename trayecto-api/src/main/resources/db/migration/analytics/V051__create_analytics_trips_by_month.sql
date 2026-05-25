CREATE TABLE analytics_trips_by_month (
    user_id     UUID NOT NULL,
    year_month  VARCHAR(7) NOT NULL,
    trip_count  INTEGER NOT NULL DEFAULT 0,
    total_km    NUMERIC(10,1) NOT NULL DEFAULT 0,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, year_month)
);
CREATE INDEX ix_atbm_year_month ON analytics_trips_by_month(year_month);
