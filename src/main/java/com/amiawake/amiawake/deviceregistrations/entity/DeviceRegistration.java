package com.amiawake.amiawake.deviceregistrations.entity;

import com.amiawake.amiawake.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "device_registrations")
public class DeviceRegistration {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "push_token", nullable = false, unique = true)
    private String pushToken;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    protected DeviceRegistration() {
    }

    public DeviceRegistration(User user, UUID deviceId, String pushToken) {
        this.user = Objects.requireNonNull(user, "User must not be null");
        this.deviceId = Objects.requireNonNull(deviceId, "Device id must not be null");
        this.pushToken = Objects.requireNonNull(pushToken, "Push token must not be null");

        if (pushToken.isBlank()) {
            throw new IllegalArgumentException("Push token must not be blank");
        }

        this.id = UUID.randomUUID();

        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updatePushToken(String pushToken) {
        Objects.requireNonNull(pushToken, "Push token must not be null");

        if (pushToken.isBlank()) {
            throw new IllegalArgumentException("Push token must not be blank");
        }

        this.pushToken = pushToken;
        this.updatedAt = Instant.now();
    }
}