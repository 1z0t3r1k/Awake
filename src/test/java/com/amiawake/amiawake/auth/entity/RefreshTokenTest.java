package com.amiawake.amiawake.auth.entity;

import com.amiawake.amiawake.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void constructorCreatesActiveToken() {
        User user = user("alice");
        Instant expiresAt = Instant.now().plusSeconds(60);

        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID(), user, "hash", expiresAt);

        assertThat(refreshToken.getId()).isNotNull();
        assertThat(refreshToken.getUser()).isSameAs(user);
        assertThat(refreshToken.getTokenHash()).isEqualTo("hash");
        assertThat(refreshToken.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(refreshToken.getCreatedAt()).isNotNull();
        assertThat(refreshToken.getRevokedAt()).isNull();
    }

    @Test
    void revokeMarksTokenRevoked() {
        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID(), user("alice"), "hash", Instant.now().plusSeconds(60));

        refreshToken.revoke();

        assertThat(refreshToken.getRevokedAt()).isNotNull();
    }

    @Test
    void rotateReplacesHashAndExpirationButKeepsTokenActive() {
        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID(), user("alice"), "old-hash", Instant.now().plusSeconds(60));
        Instant newExpiresAt = Instant.now().plusSeconds(120);

        refreshToken.rotate("new-hash", newExpiresAt);

        assertThat(refreshToken.getTokenHash()).isEqualTo("new-hash");
        assertThat(refreshToken.getExpiresAt()).isEqualTo(newExpiresAt);
        assertThat(refreshToken.getRevokedAt()).isNull();
    }

    private static User user(String username) {
        return new User(UUID.randomUUID(), username, username, "hash", "UTC");
    }
}
