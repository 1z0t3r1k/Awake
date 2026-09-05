package com.amiawake.amiawake.wakesubscription.repository;

import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.wakesubscription.entity.WakeSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WakeSubscriptionRepository extends JpaRepository<WakeSubscription, UUID> {
    boolean existsBySubscriberAndTarget(User subscriber, User target);

    List<WakeSubscription> findAllByTarget(User target);

    Optional<WakeSubscription> findBySubscriberAndTarget(User subscriber, User target);
}
