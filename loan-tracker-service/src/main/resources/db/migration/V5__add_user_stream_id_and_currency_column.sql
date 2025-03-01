-- noinspection SqlResolveForFile @ routine/"uuid_generate_v1"

ALTER TABLE user_events ADD COLUMN stream_id UUID;

UPDATE user_events SET stream_id = uuid_generate_v1() where user_events.stream_id is null;

ALTER TABLE user_events ALTER COLUMN stream_id SET NOT NULL;

CREATE UNIQUE INDEX idx_user_events_user_uid_stream_id ON user_events (uid, stream_id);

ALTER TABLE user_events ADD COLUMN currency VARCHAR(3);

--alter displayname column to be nullable
ALTER TABLE user_events ALTER COLUMN display_name DROP NOT NULL;
ALTER TABLE user_events ALTER COLUMN email_verified DROP NOT NULL;

ALTER TABLE user_events DROP CONSTRAINT user_events_uid_key;
CREATE INDEX idx_user_events_uid ON user_events (uid);