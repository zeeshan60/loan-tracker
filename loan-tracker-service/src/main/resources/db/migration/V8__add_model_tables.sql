CREATE TABLE IF NOT EXISTS user_model
(
    stream_id      UUID PRIMARY KEY    NOT NULL,
    uid            VARCHAR(255) UNIQUE NOT NULL,
    display_name   VARCHAR(255)        NOT NULL,
    phone_number   VARCHAR(255),
    email          VARCHAR(255),
    photo_url      VARCHAR,
    email_verified BOOLEAN             NOT NULL,
    created_at     TIMESTAMP           NOT NULL,
    updated_at     TIMESTAMP           NOT NULL,
    version        INT                 NOT NULL,
    currency       VARCHAR(3),
    deleted        BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_user_model_phone_number ON user_model (phone_number);
CREATE INDEX idx_user_model_email ON user_model (email);
CREATE INDEX idx_user_model_updated_at ON user_model (updated_at);
CREATE INDEX idx_user_model_deleted ON user_model (deleted);

CREATE TABLE IF NOT EXISTS friend_model
(
    stream_id           UUID PRIMARY KEY NOT NULL,
    user_uid            VARCHAR          NOT NULL,
    friend_email        VARCHAR(255),
    friend_phone_number VARCHAR(255),
    friend_display_name VARCHAR(255)     NOT NULL,
    created_at          TIMESTAMP        NOT NULL,
    updated_at          TIMESTAMP        NOT NULL,
    version             INT              NOT NULL,
    deleted             BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_friend_model_user_uid ON friend_model (user_uid);
CREATE INDEX idx_friend_model_friend_email ON friend_model (friend_email);
CREATE INDEX idx_friend_model_friend_phone_number ON friend_model (friend_phone_number);
CREATE INDEX idx_friend_model_updated_at ON friend_model (updated_at);
CREATE INDEX idx_friend_model_deleted ON friend_model (deleted);

CREATE TABLE IF NOT EXISTS transaction_model
(
    stream_id        UUID PRIMARY KEY NOT NULL,
    user_uid         VARCHAR(255)     NOT NULL,
    description      VARCHAR,
    amount           NUMERIC,
    currency         VARCHAR(255),
    transaction_type VARCHAR(255),
    recipient_id     UUID,
    created_at       TIMESTAMP        NOT NULL,
    updated_at       TIMESTAMP,
    created_by       VARCHAR(255)     NOT NULL,
    updated_by       VARCHAR(255),
    split_type       VARCHAR(255),
    total_amount     NUMERIC,
    version          INT              NOT NULL,
    history_log_id   UUID,
    deleted          BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_transaction_model_user_uid ON transaction_model (user_uid);
CREATE INDEX idx_transaction_model_recipient_id ON transaction_model (recipient_id);
CREATE INDEX idx_transaction_model_updated_at ON transaction_model (updated_at);
CREATE INDEX idx_transaction_model_deleted ON transaction_model (deleted);

CREATE TABLE IF NOT EXISTS migration_status
(
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v1(),
    version        INT       NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    finished_at    TIMESTAMP,
    entity_version BIGINT           DEFAULT 0
);