-- noinspection SqlResolveForFile @ routine/"gen_random_uuid"

CREATE TABLE commands
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    command_type VARCHAR(255) NOT NULL,
    user_id      VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    payload      JSONB
);

CREATE TABLE user_events
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uid            VARCHAR(255) UNIQUE NOT NULL,
    email          VARCHAR(255),
    phone_number   VARCHAR(255),
    display_name   VARCHAR(255)        NOT NULL,
    photo_url      VARCHAR,
    email_verified BOOLEAN             NOT NULL,
    created_at     TIMESTAMP           NOT NULL,
    version        INT                 NOT NULL,
    event_type     VARCHAR(255)        NOT NULL
);
CREATE UNIQUE INDEX idx_user_events_version_userId ON user_events (uid, version);


CREATE TABLE friend_events
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_uid            VARCHAR      NOT NULL,
    friend_email        VARCHAR(255),
    friend_phone_number VARCHAR(255),
    friend_display_name VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP    NOT NULL,
    stream_id           UUID         NOT NULL,
    version             INT          NOT NULL,
    event_type          VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX idx_friend_events_version_userId ON friend_events (stream_id, version);
CREATE INDEX idx_friend_events_created_at ON friend_events (created_at);