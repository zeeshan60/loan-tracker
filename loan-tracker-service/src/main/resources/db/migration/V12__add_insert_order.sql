ALTER TABLE user_model
    ADD COLUMN insert_order BIGSERIAL;
WITH ordered AS (SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, id) AS rn
                 FROM user_model)
UPDATE user_model t
SET insert_order = o.rn
FROM ordered o
WHERE t.id = o.id;
ALTER TABLE user_model ALTER COLUMN insert_order SET NOT NULL;
CREATE INDEX idx_user_model_insert_order ON user_model(insert_order);


ALTER TABLE user_events
    ADD COLUMN insert_order BIGSERIAL;
WITH ordered AS (SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, id) AS rn
                 FROM user_events)
UPDATE user_events t
SET insert_order = o.rn
FROM ordered o
WHERE t.id = o.id;
ALTER TABLE user_events ALTER COLUMN insert_order SET NOT NULL;
CREATE INDEX idx_user_events_insert_order ON user_events(insert_order);

ALTER TABLE friend_model
    ADD COLUMN insert_order BIGSERIAL;
WITH ordered AS (SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, id) AS rn
                 FROM friend_model)
UPDATE friend_model t
SET insert_order = o.rn
FROM ordered o
WHERE t.id = o.id;
ALTER TABLE friend_model ALTER COLUMN insert_order SET NOT NULL;
CREATE INDEX idx_friend_model_insert_order ON friend_model(insert_order);

ALTER TABLE friend_events
    ADD COLUMN insert_order BIGSERIAL;
WITH ordered AS (SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, id) AS rn
                 FROM friend_events)
UPDATE friend_events t
SET insert_order = o.rn
FROM ordered o
WHERE t.id = o.id;
ALTER TABLE friend_events ALTER COLUMN insert_order SET NOT NULL;
CREATE INDEX idx_friend_events_insert_order ON friend_events(insert_order);

ALTER TABLE transaction_events
    ADD COLUMN insert_order BIGSERIAL;
WITH ordered AS (SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, id) AS rn
                 FROM transaction_events)
UPDATE transaction_events t
SET insert_order = o.rn
FROM ordered o
WHERE t.id = o.id;
ALTER TABLE transaction_events ALTER COLUMN insert_order SET NOT NULL;
CREATE INDEX idx_transaction_events_insert_order ON transaction_events(insert_order);
