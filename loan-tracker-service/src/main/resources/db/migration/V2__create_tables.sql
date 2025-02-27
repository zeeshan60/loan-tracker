-- noinspection SqlResolveForFile @ routine/"uuid_generate_v1"

CREATE TABLE transaction_events
(
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v1(),
    user_uid         VARCHAR(255) NOT NULL,
    description      VARCHAR,
    amount           NUMERIC,
    currency         VARCHAR(255),
    transaction_type VARCHAR(255),
    recipient_id     UUID,
    created_at       TIMESTAMP    NOT NULL,
    created_by       VARCHAR(255) NOT NULL,
    split_type       VARCHAR(255),
    total_amount     NUMERIC,
    stream_id        UUID         NOT NULL,
    version          INT          NOT NULL,
    event_type       VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX idx_transaction_events_version_userId ON transaction_events (user_uid, stream_id, version);
CREATE INDEX idx_transaction_events_created_at ON transaction_events (created_at);
CREATE INDEX idx_transaction_events_user_uid ON transaction_events (user_uid);
CREATE INDEX idx_transaction_events_recipient_id ON transaction_events (recipient_id);
CREATE INDEX idx_transaction_events_event_type ON transaction_events (event_type);