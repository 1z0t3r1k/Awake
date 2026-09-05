package com.amiawake.amiawake.wakesubscription.service;

import com.amiawake.amiawake.common.exception.CannotSubscribeToSelfException;
import com.amiawake.amiawake.common.exception.WakeSubscriptionAlreadyExistsException;
import com.amiawake.amiawake.common.exception.WakeSubscriptionForbiddenException;
import com.amiawake.amiawake.friendship.service.FriendshipService;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.service.UserService;
import com.amiawake.amiawake.wakesubscription.entity.WakeSubscription;
import com.amiawake.amiawake.wakesubscription.repository.WakeSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WakeSubscriptionService {
    private final WakeSubscriptionRepository wakeSubscriptionRepository;
    private final FriendshipService friendshipService;
    private final UserService userService;

    public WakeSubscriptionService(
            WakeSubscriptionRepository wakeSubscriptionRepository,
            FriendshipService friendshipService,
            UserService userService
    ) {
        this.wakeSubscriptionRepository = wakeSubscriptionRepository;
        this.friendshipService = friendshipService;
        this.userService = userService;
    }

    @Transactional
    public WakeSubscription subscribeToWake(UUID subscriberId, UUID targetId) {
        if (subscriberId.equals(targetId)) {
            throw new CannotSubscribeToSelfException();
        }

        User subscriber = userService.getUserById(subscriberId);
        User target = userService.getUserById(targetId);

        if (wakeSubscriptionRepository.existsBySubscriberAndTarget(subscriber, target)) {
            throw new WakeSubscriptionAlreadyExistsException();
        }

        boolean areAcceptedFriends = friendshipService.areAcceptedFriends(subscriber, target);

        if (!areAcceptedFriends) {
            throw new WakeSubscriptionForbiddenException();
        }

        WakeSubscription wakeSubscription = new WakeSubscription(subscriber, target);

        return wakeSubscriptionRepository.save(wakeSubscription);
    }

    @Transactional
    public void removeWakeSubscription(UUID subscriberId, UUID targetId) {
        if (subscriberId.equals(targetId)) {
            return;
        }

        User subscriber = userService.getUserById(subscriberId);
        User target = userService.getUserById(targetId);

        Optional<WakeSubscription> optionalWakeSubscription = wakeSubscriptionRepository.findBySubscriberAndTarget(subscriber, target);

        optionalWakeSubscription.ifPresent(wakeSubscriptionRepository::delete);
    }

    public List<User> getSubscribersForTarget(User target) {
        List<WakeSubscription> wakeSubscriptions = wakeSubscriptionRepository.findAllByTarget(target);

        return wakeSubscriptions.stream()
                .map(WakeSubscription::getSubscriber)
                .toList();
    }
}
