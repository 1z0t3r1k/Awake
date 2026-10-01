TRUNCATE TABLE device_registrations;

ALTER TABLE device_registrations
    DROP COLUMN device_id,
    DROP COLUMN push_token,
    ADD COLUMN firebase_installation_id TEXT NOT NULL;

ALTER TABLE device_registrations
    ADD CONSTRAINT uk_device_registrations_firebase_installation_id
        UNIQUE (firebase_installation_id);