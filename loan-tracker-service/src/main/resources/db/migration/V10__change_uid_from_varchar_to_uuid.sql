
ALTER TABLE commands
    RENAME COLUMN user_id TO user_fb_id;
ALTER TABLE commands
    ALTER COLUMN user_fb_id DROP NOT NULL;
ALTER TABLE commands
    ADD COLUMN user_id UUID;
ALTER TABLE user_events
    ALTER COLUMN uid DROP NOT NULL;

--Change user_uid to uuid and set user stream_id as user_uid
ALTER TABLE friend_events
    ADD COLUMN user_id UUID;
UPDATE friend_events
SET user_id = (SELECT stream_id FROM user_model WHERE user_model.uid = friend_events.user_uid)
where friend_events.user_id is null;
ALTER TABLE friend_events drop user_uid;
ALTER TABLE friend_events
    RENAME COLUMN user_id TO user_uid;

-- Add created_by column to user_events and friend_events
ALTER TABLE user_events add column created_by UUID;
UPDATE user_events set created_by = stream_id where created_by is null;
ALTER TABLE user_events
    ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE friend_events add column created_by UUID;
UPDATE friend_events set created_by = friend_events.user_uid where created_by is null;
ALTER TABLE friend_events
    ALTER COLUMN created_by SET NOT NULL;

-- Change user_uid to uuid and set user stream_id as user_uid in friends model
ALTER TABLE friend_model
    ADD COLUMN user_id UUID;
UPDATE friend_model
SET user_id = (SELECT stream_id FROM user_model WHERE user_model.uid = friend_model.user_uid)
where friend_model.user_id is null;
ALTER TABLE friend_model drop user_uid;
ALTER TABLE friend_model
    RENAME COLUMN user_id TO user_uid;

-- Change user_uid to uuid and set user stream_id as user_uid in transaction events
ALTER TABLE transaction_events
    ADD COLUMN user_id UUID;
UPDATE transaction_events
SET user_id = (SELECT stream_id FROM user_model WHERE user_model.uid = transaction_events.user_uid)
where transaction_events.user_id is null;
ALTER TABLE transaction_events drop user_uid;
ALTER TABLE transaction_events
    RENAME COLUMN user_id TO user_uid;
ALTER TABLE transaction_events ALTER COLUMN user_uid SET NOT NULL;
--Transaction events are unique by user_uid, stream_id and version not just stream_id and version. due to cross transaction mechanic
CREATE UNIQUE INDEX idx_transaction_events_version_userId ON transaction_events (user_uid, stream_id, version);

ALTER TABLE transaction_events ALTER COLUMN transaction_date DROP NOT NULL;