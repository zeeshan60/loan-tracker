CREATE TABLE events
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event      VARCHAR(255) NOT NULL,
    user_id    VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    payload    JSONB
);

CREATE INDEX idx_events_created_at ON events (created_at);

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

CREATE TABLE user_friends
(
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID         NOT NULL
        CONSTRAINT fk_user_friends_user_id REFERENCES user_events (id) ON DELETE CASCADE,
    friend_id                UUID,
    friend_email             VARCHAR(255),
    friend_phone_number      VARCHAR(255),
    friend_display_name      VARCHAR(255) NOT NULL,
    friend_total_amounts_dto JSONB,
    created_at               TIMESTAMP    NOT NULL,
    updated_at               TIMESTAMP    NOT NULL
);

CREATE INDEX idx_user_friends_user_id ON user_friends (user_id);
CREATE UNIQUE INDEX idx_user_friends_friend_id_user_id ON user_friends (friend_id, user_id);
CREATE UNIQUE INDEX idx_user_friends_friend_email_user_id ON user_friends (friend_email, user_id);
CREATE UNIQUE INDEX idx_user_friends_friend_phone_number_user_id ON user_friends (friend_phone_number, user_id);


CREATE TABLE friend_events
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_uid            VARCHAR      NOT NULL,
    friend_email        VARCHAR(255),
    friend_phone_number VARCHAR(255),
    friend_display_name VARCHAR(255) NOT NULL,
    friend_photo_url    TEXT,
    created_at          TIMESTAMP    NOT NULL,
    stream_id           UUID         NOT NULL,
    version             INT          NOT NULL,
    event_type          VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX idx_friend_events_version_userId ON friend_events (stream_id, version);
CREATE INDEX idx_friend_events_created_at ON friend_events (created_at);