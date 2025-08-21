CREATE TABLE group_events
(
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v1(),
    name         VARCHAR(255) NOT NULL,
    description  VARCHAR,
    event_type   VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    created_by   UUID         NOT NULL,
    stream_id    UUID         NOT NULL,
    version      INT          NOT NULL,
    insert_order bigserial
);

CREATE INDEX idx_group_events_created_at ON group_events (created_at);
CREATE INDEX idx_group_events_insert_order ON group_events (insert_order);
CREATE UNIQUE INDEX idx_group_events_version_userid ON group_events (stream_id, version);

CREATE TABLE group_model
(
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v1(),
    name         VARCHAR(255) NOT NULL,
    description  VARCHAR,
    stream_id    UUID UNIQUE  NOT NULL,
    version      INT          NOT NULL,
    deleted      BOOLEAN          DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    created_by   UUID         NOT NULL,
    updated_by   UUID         NOT NULL,
    insert_order bigserial
);

CREATE INDEX idx_group_model_stream_id ON group_model (stream_id);
CREATE INDEX idx_group_model_deleted ON group_model (deleted);
CREATE INDEX idx_group_model_created_at ON group_model (created_at);
CREATE INDEX idx_group_model_updated_at ON group_model (updated_at);
CREATE INDEX idx_group_model_insert_order ON group_model (insert_order);