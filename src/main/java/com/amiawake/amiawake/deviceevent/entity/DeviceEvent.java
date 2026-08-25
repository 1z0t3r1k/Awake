package com.amiawake.amiawake.deviceevent.entity;

import com.amiawake.amiawake.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "device_events")
public class DeviceEvent {
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DeviceEventType type;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected DeviceEvent() {
    }

    public DeviceEvent(
            UUID eventId,
            User user,
            DeviceEventType type,
            Instant occurredAt
    ) {
        Objects.requireNonNull(eventId, "Event id must not be null");
        Objects.requireNonNull(user, "User must not be null");
        Objects.requireNonNull(type, "Device event type must not be null");
        Objects.requireNonNull(occurredAt, "Occurred time must not be null");

        this.eventId = eventId;
        this.user = user;
        this.type = type;
        this.occurredAt = occurredAt;
        this.receivedAt = Instant.now();
    }
}
