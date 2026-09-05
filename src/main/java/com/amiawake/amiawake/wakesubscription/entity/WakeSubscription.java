package com.amiawake.amiawake.wakesubscription.entity;

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
@Table(name = "wake_subscriptions")
public class WakeSubscription {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", nullable = false)
    private User subscriber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WakeSubscription() {
    }

    public WakeSubscription(User subscriber, User target) {
        Objects.requireNonNull(subscriber, "Subscriber must not be null");
        Objects.requireNonNull(target, "Subscription target must not be null");

        if (subscriber.getId().equals(target.getId())) {
            throw new IllegalArgumentException(
                    "Subscriber cannot subscribe to themselves"
            );
        }

        this.id = UUID.randomUUID();
        this.subscriber = subscriber;
        this.target = target;
        this.createdAt = Instant.now();
    }
}
