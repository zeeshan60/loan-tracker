ALTER TABLE transaction_events
    add column group_id UUID;

CREATE INDEX idx_transaction_events_group_id ON transaction_events (group_id);