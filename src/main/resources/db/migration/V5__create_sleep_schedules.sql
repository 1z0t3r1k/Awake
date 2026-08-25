CREATE TABLE sleep_schedules
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL,
    sleep_time TIME        NOT NULL,
    wake_time  TIME        NOT NULL,
    enabled    BOOLEAN     NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE (user_id),

    CONSTRAINT check_different_sleep_and_wake_time
        CHECK (sleep_time <> wake_time)
);