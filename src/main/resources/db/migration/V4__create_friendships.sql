CREATE TABLE friendships
(
    id           UUID PRIMARY KEY,
    user1_id     UUID        NOT NULL,
    user2_id     UUID        NOT NULL,
    requester_id UUID        NOT NULL,
    status       VARCHAR(32) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,

    FOREIGN KEY (user1_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (requester_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT check_different_users
        CHECK (user1_id <> user2_id),

    CONSTRAINT check_requester_in_pair
        CHECK (requester_id = user1_id OR requester_id = user2_id)
);

CREATE UNIQUE INDEX unique_friendship_pair
    ON friendships (
                    LEAST(user1_id, user2_id),
                    GREATEST(user1_id, user2_id)
        );