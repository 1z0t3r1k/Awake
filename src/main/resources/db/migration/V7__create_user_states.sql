CREATE TABLE user_states
(
    user_id       UUID PRIMARY KEY,
    sleep_state   VARCHAR(32)      NOT NULL,
    confidence    DOUBLE PRECISION NOT NULL,
    calculated_at TIMESTAMPTZ      NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CHECK (confidence >= 0.0 AND confidence <= 1.0)
);