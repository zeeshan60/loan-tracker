CREATE TABLE transactions
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    amount            NUMERIC      NOT NULL,
    currency          VARCHAR(3)   NOT NULL,
    date              TIMESTAMP    NOT NULL,
    description       VARCHAR(255) NOT NULL,
    type              VARCHAR(255) NOT NULL,
    user_id           UUID         NOT NULL
        CONSTRAINT fk_transactions_user_id REFERENCES users (id) ON DELETE CASCADE,
    friend_id         UUID
        CONSTRAINT fk_transactions_friend_id REFERENCES users (id) ON DELETE SET NULL,
    friend_email      VARCHAR(255),
    friend_phone      VARCHAR(255),
    transaction_trail JSONB
);