package com.amiawake.amiawake.userstate.entity;

import com.amiawake.amiawake.inference.states.SleepState;
import com.amiawake.amiawake.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "user_states")
public class UserState {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "sleep_state", nullable = false)
    private SleepState sleepState;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    protected UserState() {
    }

    public UserState(User user, SleepState sleepState, double confidence) {
        Objects.requireNonNull(user, "User must not be null");
        Objects.requireNonNull(sleepState, "Sleep state must not be null");

        validateConfidence(confidence);

        this.user = user;
        this.sleepState = sleepState;
        this.confidence = confidence;
        this.calculatedAt = Instant.now();
    }

    public void updateState(SleepState sleepState, double confidence) {
        Objects.requireNonNull(sleepState, "Sleep state must not be null");

        validateConfidence(confidence);

        this.sleepState = sleepState;
        this.confidence = confidence;
        this.calculatedAt = Instant.now();
    }

    private void validateConfidence(double confidence) {
        if (Double.isNaN(confidence) || confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException();
        }
    }
}
