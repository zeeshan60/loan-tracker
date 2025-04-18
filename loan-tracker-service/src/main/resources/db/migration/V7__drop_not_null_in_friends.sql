-- noinspection SqlResolveForFile @ routine/"uuid_generate_v1"
ALTER TABLE friend_events
ALTER COLUMN friend_display_name DROP NOT NULL;