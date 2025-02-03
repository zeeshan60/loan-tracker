CREATE TABLE events (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        event VARCHAR(255) NOT NULL,
                        event_id VARCHAR(255) NOT NULL,
                        user_id VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP NOT NULL,
                        payload JSONB
);

CREATE INDEX idx_events_created_at ON events(created_at);