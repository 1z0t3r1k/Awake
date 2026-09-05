CREATE TABLE wake_subscriptions
(
    id            UUID PRIMARY KEY,
    subscriber_id UUID        NOT NULL,
    target_id     UUID        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,

    FOREIGN KEY (subscriber_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (target_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT check_different_users
        CHECK (subscriber_id <> target_id),
    UNIQUE (subscriber_id, target_id)
);

CREATE INDEX idx_wake_subscriptions_target_id
    ON wake_subscriptions (target_id);