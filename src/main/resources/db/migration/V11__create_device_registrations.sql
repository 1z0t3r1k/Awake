CREATE TABLE device_registrations
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL,
    device_id  UUID        NOT NULL,
    push_token TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE (push_token),
    unique (user_id, device_id)
);

CREATE INDEX index_user_id ON device_registrations (user_id);