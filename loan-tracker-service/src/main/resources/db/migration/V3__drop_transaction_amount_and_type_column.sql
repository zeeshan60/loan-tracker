-- noinspection SqlResolveForFile @ routine/"gen_random_uuid"

ALTER TABLE transaction_events DROP COLUMN amount;
ALTER TABLE transaction_events DROP COLUMN transaction_type;