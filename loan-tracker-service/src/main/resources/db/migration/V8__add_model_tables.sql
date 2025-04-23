CREATE TABLE IF NOT EXISTS user_model
(
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v1(),
    stream_id      UUID UNIQUE         NOT NULL,
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
    deleted        BOOLEAN          DEFAULT FALSE
);
CREATE INDEX idx_user_model_phone_number ON user_model (phone_number);
CREATE INDEX idx_user_model_email ON user_model (email);
CREATE INDEX idx_user_model_updated_at ON user_model (updated_at);
CREATE INDEX idx_user_model_deleted ON user_model (deleted);

CREATE TABLE IF NOT EXISTS friend_model
(
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v1(),
    stream_id           UUID UNIQUE  NOT NULL,
    user_uid            VARCHAR      NOT NULL,
    friend_email        VARCHAR(255),
    friend_phone_number VARCHAR(255),
    friend_display_name VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL,
    version             INT          NOT NULL,
    deleted             BOOLEAN          DEFAULT FALSE
);

CREATE INDEX idx_friend_model_user_uid ON friend_model (user_uid);
CREATE INDEX idx_friend_model_friend_email ON friend_model (friend_email);
CREATE INDEX idx_friend_model_friend_phone_number ON friend_model (friend_phone_number);
CREATE INDEX idx_friend_model_updated_at ON friend_model (updated_at);
CREATE INDEX idx_friend_model_deleted ON friend_model (deleted);


CREATE TABLE IF NOT EXISTS migration_status
(
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v1(),
    version        INT       NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    finished_at    TIMESTAMP,
    entity_version BIGINT           DEFAULT 0
);