package com.amiawake.amiawake.sleepschedule.entity;

import com.amiawake.amiawake.common.exception.InvalidSleepScheduleException;
import com.amiawake.amiawake.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "sleep_schedules")
public class SleepSchedule {
    @Id
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "sleep_time", nullable = false)
    private LocalTime sleepTime;

    @Column(name = "wake_time", nullable = false)
    private LocalTime wakeTime;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SleepSchedule() {
    }

    public SleepSchedule(
            User user,
            LocalTime sleepTime,
            LocalTime wakeTime
    ) {
        this.user = Objects.requireNonNull(user, "User must not be null");
        this.sleepTime = Objects.requireNonNull(sleepTime, "Sleep time must not be null");
        this.wakeTime = Objects.requireNonNull(wakeTime, "Wake time must not be null");

        if (sleepTime.equals(wakeTime)) {
            throw new InvalidSleepScheduleException();
        }

        this.id = UUID.randomUUID();
        this.enabled = true;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void changeSchedule(LocalTime sleepTime, LocalTime wakeTime) {
        LocalTime newSleepTime = Objects.requireNonNull(
                sleepTime,
                "Sleep time must not be null"
        );

        LocalTime newWakeTime = Objects.requireNonNull(
                wakeTime,
                "Wake time must not be null"
        );

        if (newSleepTime.equals(newWakeTime)) {
            throw new InvalidSleepScheduleException();
        }

        this.sleepTime = newSleepTime;
        this.wakeTime = newWakeTime;
        this.updatedAt = Instant.now();
    }

    public void changeEnabled(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }

    public boolean isSleepingAt(LocalTime currentTime) {
        Objects.requireNonNull(currentTime, "Current time must not be null");
        if (sleepTime.isBefore(wakeTime)) {
            return (currentTime.isAfter(sleepTime) || currentTime.equals(sleepTime))
                    && currentTime.isBefore(wakeTime);
        } else {
            return currentTime.isAfter(sleepTime)
                    || currentTime.equals(sleepTime)
                    || currentTime.isBefore(wakeTime);
        }
    }
}
