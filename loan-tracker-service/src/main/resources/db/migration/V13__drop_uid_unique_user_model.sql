-- Drop the existing unique constraint or index on uid
ALTER TABLE user_model DROP CONSTRAINT user_model_uid_key;

-- Create a partial unique index for uid where deleted is false
CREATE UNIQUE INDEX idx_user_model_uid_not_deleted
    ON user_model (uid)
    WHERE deleted = FALSE;