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

    @Column(name = "firebase_installation_id", nullable = false, unique = true)
    private String firebaseInstallationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_registered_at", nullable = false)
    private Instant lastRegisteredAt;

    protected DeviceRegistration() {
    }

    public DeviceRegistration(User user, String firebaseInstallationId) {
        this.user = Objects.requireNonNull(user, "User must not be null");
        Objects.requireNonNull(firebaseInstallationId, "Firebase installation id must not be null");

        if (firebaseInstallationId.isBlank()) {
            throw new IllegalArgumentException("Firebase installation id must not be blank");
        }

        this.id = UUID.randomUUID();
        this.firebaseInstallationId = firebaseInstallationId;

        Instant now = Instant.now();
        this.createdAt = now;
        this.lastRegisteredAt = now;
    }

    public void refresh() {
        this.lastRegisteredAt = Instant.now();
    }

    public void reassignTo(User user) {
        Objects.requireNonNull(user, "User must not be null");

        if (!this.user.getId().equals(user.getId())) {
            this.user = user;
        }
    }
}