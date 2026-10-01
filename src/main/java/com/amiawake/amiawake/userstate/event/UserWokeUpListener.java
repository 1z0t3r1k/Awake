package com.amiawake.amiawake.userstate.event;

import com.amiawake.amiawake.user.projection.UserNotificationInfo;
import com.amiawake.amiawake.user.service.UserService;
import com.amiawake.amiawake.wakesubscription.service.WakeNotificationService;
import com.amiawake.amiawake.wakesubscription.service.WakeSubscriptionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Component
public class UserWokeUpListener {
    private final WakeNotificationService wakeNotificationService;
    private final WakeSubscriptionService wakeSubscriptionService;
    private final UserService userService;

    public UserWokeUpListener(
            WakeNotificationService wakeNotificationService,
            WakeSubscriptionService wakeSubscriptionService, UserService userService
    ) {
        this.wakeNotificationService = wakeNotificationService;
        this.wakeSubscriptionService = wakeSubscriptionService;
        this.userService = userService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserWokeUp(UserWokeUpEvent event) {
        UserNotificationInfo targetInfo = userService.getUserNotificationInfoById(event.userId());

        List<UUID> subscribersIds = wakeSubscriptionService.getSubscribersAndRemoveSubscriptions(event.userId());

        wakeNotificationService.sendWakeNotifications(targetInfo, subscribersIds);
    }
}
