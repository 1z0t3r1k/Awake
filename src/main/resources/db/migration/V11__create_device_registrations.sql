CREATE TABLE device_registrations
(
    id                       UUID PRIMARY KEY,
    user_id                  UUID        NOT NULL,
    firebase_installation_id TEXT        NOT NULL,
    created_at               TIMESTAMPTZ NOT NULL,
    last_registered_at       TIMESTAMPTZ NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE (firebase_installation_id),
    unique (user_id, firebase_installation_id)
);

CREATE INDEX index_user_id ON device_registrations (user_id);