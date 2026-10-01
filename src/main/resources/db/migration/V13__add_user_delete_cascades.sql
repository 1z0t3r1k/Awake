ALTER TABLE refresh_tokens
    DROP CONSTRAINT refresh_tokens_user_id_fkey,
    ADD CONSTRAINT refresh_tokens_user_id_fkey
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;

ALTER TABLE sleep_classification_events
    DROP CONSTRAINT sleep_classification_events_user_id_fkey,
    ADD CONSTRAINT sleep_classification_events_user_id_fkey
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;