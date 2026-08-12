CREATE TABLE users
(
    id            UUID PRIMARY KEY,
    username      VARCHAR(32)  NOT NULL,
    display_name  VARCHAR(80)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    time_zone     VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT chk_users_username_format
        CHECK (username ~ '^[a-z0-9._]{3,32}$'
) ,
    CONSTRAINT chk_users_display_name_not_blank
        CHECK (length(trim(display_name)) > 0),
    CONSTRAINT chk_users_updated_at
        CHECK (updated_at >= created_at)
);
