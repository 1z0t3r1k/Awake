CREATE TABLE sleep_classification_events
(
    id               UUID PRIMARY KEY,
    user_id          UUID        NOT NULL,
    occurred_at      TIMESTAMPTZ NOT NULL,
    received_at      TIMESTAMPTZ NOT NULL,
    sleep_confidence INTEGER     NOT NULL,
    motion           INTEGER     NOT NULL,
    light            INTEGER     NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id),

    CONSTRAINT check_sleep_confidence_range
        CHECK (sleep_confidence BETWEEN 0 AND 100)
);