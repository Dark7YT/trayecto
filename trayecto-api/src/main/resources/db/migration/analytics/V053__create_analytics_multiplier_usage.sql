CREATE TABLE analytics_multiplier_usage (
    user_id         UUID NOT NULL,
    multiplier_id   UUID NOT NULL,
    multiplier_rate NUMERIC(4,2) NOT NULL,
    usage_count     INTEGER NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, multiplier_id)
);
