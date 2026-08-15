package com.amiawake.amiawake.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "users")
public class User {
    @Id
    private UUID id;
    @Column(name = "username", nullable = false, length = 32)
    private String username;
    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "time_zone", nullable = false, length = 100)
    private String timeZone;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AvailabilityStatus status;

    protected User() {
    }

    public User(UUID id, String username, String displayName, String passwordHash, String timeZone) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.timeZone = timeZone;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = AvailabilityStatus.AVAILABLE;
    }

    public void changeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }

        this.username = username;
    }

    public void changeStatus(AvailabilityStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        this.status = status;
        this.updatedAt = Instant.now();
    }

    public enum AvailabilityStatus {
        AVAILABLE,
        TEXT_ONLY,
        DO_NOT_DISTURB,
        SLEEPING
    }
}
