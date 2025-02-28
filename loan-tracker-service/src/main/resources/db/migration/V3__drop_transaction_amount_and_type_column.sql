-- noinspection SqlResolveForFile @ routine/"uuid_generate_v1"

ALTER TABLE transaction_events DROP COLUMN amount;
ALTER TABLE transaction_events DROP COLUMN transaction_type;