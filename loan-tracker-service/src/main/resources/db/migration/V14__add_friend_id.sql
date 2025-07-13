ALTER TABLE friend_events
    ADD COLUMN friend_id UUID;

UPDATE friend_events
SET friend_id = (select stream_id
                 from user_model um
                 where (um.email is not null and um.email = friend_events.friend_email)
                    or (um.phone_number is not null and um.phone_number = friend_events.friend_phone_number) limit 1)
WHERE friend_id IS NULL and friend_events.event_type = 'FRIEND_CREATED';

ALTER TABLE friend_model
    ADD COLUMN friend_id UUID;
CREATE INDEX idx_friend_events_friend_id ON friend_model(friend_id);