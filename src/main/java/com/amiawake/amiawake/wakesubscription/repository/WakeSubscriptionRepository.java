package com.amiawake.amiawake.wakesubscription.repository;

import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.wakesubscription.entity.WakeSubscription;
import com.amiawake.amiawake.wakesubscription.projection.WakeSubscriptionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WakeSubscriptionRepository extends JpaRepository<WakeSubscription, UUID> {
    boolean existsBySubscriberAndTarget(User subscriber, User target);

    @Query("""
            select new com.amiawake.amiawake.wakesubscription.projection.WakeSubscriptionInfo(
                ws.subscriber.id,
                ws.id
            )
            from WakeSubscription ws
            where ws.target.id = :targetId
            """)
    List<WakeSubscriptionInfo> findWakeSubscriptionInfosByTargetId(UUID targetId);

    @Modifying
    @Query("""
            delete WakeSubscription ws
            where ws.id in :subscriptionIds 
            """)
    void deleteAllByIdIn(Collection<UUID> subscriptionIds);

    Optional<WakeSubscription> findBySubscriberAndTarget(User subscriber, User target);
}
