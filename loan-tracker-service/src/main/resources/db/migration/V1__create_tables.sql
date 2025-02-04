CREATE TABLE events
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event      VARCHAR(255) NOT NULL,
    event_id   VARCHAR(255) NOT NULL,
    user_id    VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    payload    JSONB,
    source     VARCHAR(255)
);

CREATE INDEX idx_events_created_at ON events (created_at);

CREATE TABLE users
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uid            VARCHAR(255) UNIQUE NOT NULL,
    email          VARCHAR(255),
    phone_number   VARCHAR(255),
    display_name   VARCHAR(255)        NOT NULL,
    photo_url      TEXT,
    email_verified BOOLEAN             NOT NULL,
    created_at     TIMESTAMP           NOT NULL,
    updated_at     TIMESTAMP           NOT NULL,
    last_login_at  TIMESTAMP
);
CREATE INDEX idx_users_uid ON users (uid);

CREATE TABLE user_friends
(
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID         NOT NULL
        CONSTRAINT fk_user_friends_user_id REFERENCES users (id) ON DELETE CASCADE,
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
