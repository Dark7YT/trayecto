CREATE TABLE analytics_cost_by_month (
    user_id             UUID NOT NULL,
    year_month          VARCHAR(7) NOT NULL,
    total_cost_amount   NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_cost_currency VARCHAR(3) NOT NULL DEFAULT 'PEN',
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, year_month)
);
