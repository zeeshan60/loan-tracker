-- noinspection SqlResolveForFile @ routine/"gen_random_uuid"

ALTER TABLE transaction_events ADD COLUMN transaction_date TIMESTAMP;

UPDATE transaction_events SET transaction_date = created_at where transaction_events.transaction_date is null;

ALTER TABLE transaction_events ALTER COLUMN transaction_date SET NOT NULL;

CREATE INDEX idx_transaction_events_user_uid_transaction_date ON transaction_events (user_uid, transaction_date);