-- noinspection SqlResolveForFile @ routine/"uuid_generate_v1"
DROP INDEX idx_user_events_user_uid_stream_id;
CREATE UNIQUE INDEX idx_user_events_stream_id_version ON user_events (stream_id, version);