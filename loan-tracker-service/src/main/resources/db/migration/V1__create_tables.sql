CREATE TABLE events
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event      VARCHAR(255) NOT NULL,
    event_id   VARCHAR(255) NOT NULL,
    user_id    VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    payload    JSONB
);

CREATE INDEX idx_events_created_at ON events (created_at);

CREATE TABLE users
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uid            VARCHAR(255) UNIQUE NOT NULL,
    email          VARCHAR(255),
    display_name   VARCHAR(255)        NOT NULL,
    photo_url      TEXT,
    email_verified BOOLEAN             NOT NULL,
    created_at     TIMESTAMP           NOT NULL,
    updated_at     TIMESTAMP           NOT NULL,
    last_login_at  TIMESTAMP
);
CREATE INDEX idx_users_uid ON users (uid);