package com.amiawake.amiawake.user.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void constructorCreatesAvailableUserWithTimestamps() {
        UUID id = UUID.randomUUID();

        User user = new User(id, "alice", "Alice", "hash", "UTC");

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getDisplayName()).isEqualTo("Alice");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getTimeZone()).isEqualTo("UTC");
        assertThat(user.getStatus()).isEqualTo(User.AvailabilityStatus.AVAILABLE);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(user.getCreatedAt());
    }

    @Test
    void changeUsernameRejectsBlankValue() {
        User user = user("alice");

        assertThatThrownBy(() -> user.changeUsername(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username cannot be blank");
    }

    @Test
    void changeStatusUpdatesStatusAndUpdatedAt() {
        User user = user("alice");
        Instant previousUpdatedAt = user.getUpdatedAt();

        user.changeStatus(User.AvailabilityStatus.SLEEPING);

        assertThat(user.getStatus()).isEqualTo(User.AvailabilityStatus.SLEEPING);
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(previousUpdatedAt);
    }

    @Test
    void changeStatusRejectsNull() {
        User user = user("alice");

        assertThatThrownBy(() -> user.changeStatus(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Status cannot be null");
    }

    private static User user(String username) {
        return new User(UUID.randomUUID(), username, username, "hash", "UTC");
    }
}
