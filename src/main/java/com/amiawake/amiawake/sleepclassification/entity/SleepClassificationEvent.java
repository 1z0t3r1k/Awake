package com.amiawake.amiawake.sleepclassification.entity;

import com.amiawake.amiawake.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "sleep_classification_events")
public class SleepClassificationEvent {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "sleep_confidence", nullable = false)
    private int sleepConfidence;

    @Column(name = "motion", nullable = false)
    private int motion;

    @Column(name = "light", nullable = false)
    private int light;

    protected SleepClassificationEvent() {
    }

    public SleepClassificationEvent(
            User user,
            Instant occurredAt,
            int sleepConfidence,
            int motion,
            int light
    ) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at must not be null");
        }

        if (sleepConfidence < 0 || sleepConfidence > 100) {
            throw new IllegalArgumentException(
                    "Sleep confidence must be between 0 and 100"
            );
        }

        this.id = UUID.randomUUID();
        this.user = user;
        this.occurredAt = occurredAt;
        this.receivedAt = Instant.now();
        this.sleepConfidence = sleepConfidence;
        this.motion = motion;
        this.light = light;
    }
}
