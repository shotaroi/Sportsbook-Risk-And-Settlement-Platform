-- Seed customer and initial ledger credit for testing
INSERT INTO customers (name) VALUES ('Test Customer');
INSERT INTO ledger_entries (customer_id, type, amount, currency, reference_type, reference_id)
VALUES (1, 'CREDIT', 10000.00, 'SEK', 'BET_REFUND', 'SEED-1');
