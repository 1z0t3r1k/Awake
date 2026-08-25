CREATE TABLE device_events
(
    event_id    UUID PRIMARY KEY,
    user_id     UUID        NOT NULL,
    type        VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_device_events_user_occurred
    ON device_events (user_id, occurred_at);