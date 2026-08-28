UPDATE users
SET status = 'DO_NOT_DISTURB'
WHERE status = 'SLEEPING';

ALTER TABLE users
    ADD CONSTRAINT chk_users_status
        CHECK (status IN ('AVAILABLE', 'TEXT_ONLY', 'DO_NOT_DISTURB'));