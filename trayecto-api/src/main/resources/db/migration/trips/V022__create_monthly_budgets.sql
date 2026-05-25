CREATE TABLE trips_monthly_budgets (
    id                      UUID                     PRIMARY KEY,
    user_id                 UUID                     NOT NULL,
    year                    INTEGER                  NOT NULL,
    month                   INTEGER                  NOT NULL,
    amount_value            NUMERIC(12,2)            NOT NULL,
    amount_currency         VARCHAR(3)               NOT NULL,
    current_spend_value     NUMERIC(12,2)            NOT NULL DEFAULT 0,
    current_spend_currency  VARCHAR(3)               NOT NULL,
    warning_sent            BOOLEAN                  NOT NULL DEFAULT FALSE,
    exceeded_sent           BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_trips_budget_month       CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT ck_trips_budget_year        CHECK (year BETWEEN 2020 AND 2100),
    CONSTRAINT ck_trips_budget_amount_pos  CHECK (amount_value >= 0),
    CONSTRAINT ck_trips_budget_spend_pos   CHECK (current_spend_value >= 0),
    CONSTRAINT ux_trips_budget_user_period UNIQUE (user_id, year, month)
);

CREATE INDEX ix_trips_budget_user ON trips_monthly_budgets (user_id);
