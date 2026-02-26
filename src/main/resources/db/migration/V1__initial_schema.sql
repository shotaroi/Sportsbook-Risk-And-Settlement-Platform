-- Sportsbook Risk & Settlement Platform - Initial Schema
-- Currency: SEK, scale 2 for money

-- Customers
CREATE TABLE customers (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Wallets (one per customer per currency; ledger derives balance)
CREATE TABLE wallets (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    currency        VARCHAR(3) NOT NULL DEFAULT 'SEK',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (customer_id, currency)
);

CREATE INDEX idx_wallets_customer ON wallets(customer_id);

-- Immutable ledger entries (append-only; balance derived by sum)
CREATE TABLE ledger_entries (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    type            VARCHAR(50) NOT NULL,  -- DEBIT, CREDIT, REFUND
    amount          NUMERIC(19, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'SEK',
    reference_type  VARCHAR(50) NOT NULL,  -- BET_STAKE, BET_PAYOUT, BET_REFUND
    reference_id    VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_customer_created ON ledger_entries(customer_id, created_at);
CREATE INDEX idx_ledger_reference ON ledger_entries(reference_type, reference_id);

-- Bets
CREATE TABLE bets (
    id                  BIGSERIAL PRIMARY KEY,
    customer_id         BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    event_id            VARCHAR(255) NOT NULL,
    market_type         VARCHAR(50) NOT NULL,
    selection           VARCHAR(50) NOT NULL,
    odds                NUMERIC(10, 3) NOT NULL,
    stake               NUMERIC(19, 2) NOT NULL,
    status              VARCHAR(50) NOT NULL,  -- PLACED, SETTLED_WON, SETTLED_LOST, SETTLED_VOID
    potential_payout    NUMERIC(19, 2) NOT NULL,
    placed_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    settled_at          TIMESTAMP WITH TIME ZONE,
    settlement_batch_id VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bets_customer ON bets(customer_id);
CREATE INDEX idx_bets_event_status ON bets(event_id, status);
CREATE INDEX idx_bets_settlement_batch ON bets(settlement_batch_id) WHERE settlement_batch_id IS NOT NULL;

-- Exposures (per event, market, selection) - optimistic locking
CREATE TABLE exposures (
    id                  BIGSERIAL PRIMARY KEY,
    event_id            VARCHAR(255) NOT NULL,
    market_type         VARCHAR(50) NOT NULL,
    selection           VARCHAR(50) NOT NULL,
    reserved_liability  NUMERIC(19, 2) NOT NULL DEFAULT 0,
    version             BIGINT NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (event_id, market_type, selection)
);

CREATE INDEX idx_exposures_event ON exposures(event_id);

-- Limits (scope: GLOBAL, EVENT, or EVENT_MARKET_SELECTION)
CREATE TABLE limits (
    id                      BIGSERIAL PRIMARY KEY,
    scope_type              VARCHAR(50) NOT NULL,  -- GLOBAL, EVENT, EVENT_MARKET_SELECTION
    scope_id                VARCHAR(255),          -- eventId or eventId|marketType|selection
    max_reserved_liability  NUMERIC(19, 2),
    max_stake_per_bet       NUMERIC(19, 2),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_limits_scope ON limits(scope_type, scope_id);

-- Idempotency keys
CREATE TABLE idempotency_keys (
    id              BIGSERIAL PRIMARY KEY,
    scope           VARCHAR(50) NOT NULL,   -- BET_PLACEMENT, RESULT_INGEST
    scope_id        VARCHAR(255) NOT NULL,   -- customerId or eventId
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash    VARCHAR(64) NOT NULL,
    response_json   TEXT,
    status          VARCHAR(50) NOT NULL,   -- COMPLETED, REJECTED
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (scope, scope_id, idempotency_key)
);

CREATE INDEX idx_idempotency_lookup ON idempotency_keys(scope, scope_id, idempotency_key);

-- Event results (for settlement idempotency)
CREATE TABLE event_results (
    id              BIGSERIAL PRIMARY KEY,
    event_id        VARCHAR(255) NOT NULL UNIQUE,
    winning_selection VARCHAR(50),          -- HOME, DRAW, AWAY or NULL for VOID
    result_version  BIGINT NOT NULL DEFAULT 1,
    settled_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_event_results_event ON event_results(event_id);
