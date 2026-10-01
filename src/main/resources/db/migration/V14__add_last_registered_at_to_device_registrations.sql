ALTER TABLE device_registrations
    ADD COLUMN last_registered_at TIMESTAMPTZ NOT NULL;