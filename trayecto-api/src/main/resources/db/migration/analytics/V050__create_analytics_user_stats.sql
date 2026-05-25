CREATE TABLE analytics_user_stats (
    user_id              UUID PRIMARY KEY,
    total_trips          INTEGER NOT NULL DEFAULT 0,
    total_completed      INTEGER NOT NULL DEFAULT 0,
    total_km             NUMERIC(12,1) NOT NULL DEFAULT 0,
    total_cost_amount    NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_cost_currency  VARCHAR(3) NOT NULL DEFAULT 'PEN',
    favorite_multiplier_id UUID,
    last_completed_at    TIMESTAMP WITH TIME ZONE,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL
);
