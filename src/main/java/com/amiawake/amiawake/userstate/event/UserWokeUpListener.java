package com.amiawake.amiawake.userstate.event;

import com.amiawake.amiawake.wakesubscription.service.WakeNotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserWokeUpListener {
    private final WakeNotificationService wakeNotificationService;

    public UserWokeUpListener(WakeNotificationService wakeNotificationService) {
        this.wakeNotificationService = wakeNotificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserWokeUp(UserWokeUpEvent event) {
        wakeNotificationService.sendWakeNotifications(event.user());
    }
}
