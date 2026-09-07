package com.amiawake.amiawake.wakesubscription.service;

import com.amiawake.amiawake.common.exception.PushNotificationException;
import com.amiawake.amiawake.deviceregistrations.entity.DeviceRegistration;
import com.amiawake.amiawake.deviceregistrations.service.DeviceRegistrationService;
import com.amiawake.amiawake.notification.service.NotificationService;
import com.amiawake.amiawake.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WakeNotificationService {
    private final DeviceRegistrationService deviceRegistrationService;
    private final WakeSubscriptionService wakeSubscriptionService;
    private final NotificationService notificationService;
    private static final Logger log =
            LoggerFactory.getLogger(WakeNotificationService.class);

    public WakeNotificationService(
            WakeSubscriptionService wakeSubscriptionService, DeviceRegistrationService deviceRegistrationService,
            NotificationService notificationService
    ) {
        this.deviceRegistrationService = deviceRegistrationService;
        this.wakeSubscriptionService = wakeSubscriptionService;
        this.notificationService = notificationService;
    }

    public List<String> getWakeNotificationRecipients(User target) {
        List<User> subscribers = wakeSubscriptionService.getSubscribersForTarget(target);
        List<String> firebaseInstallationIds = new ArrayList<>();

        for (User subscriber : subscribers) {
            List<DeviceRegistration> deviceRegistrations = deviceRegistrationService.getUserDeviceRegistrations(subscriber);

            for (DeviceRegistration deviceRegistration : deviceRegistrations) {
                firebaseInstallationIds.add(deviceRegistration.getFirebaseInstallationId());
            }
        }

        return firebaseInstallationIds;
    }

    public void sendWakeNotifications(User target) {
        List<String> subscribersFIDs = getWakeNotificationRecipients(target);

        String title = target.getDisplayName() + " похоже, уже не спит";

        String body = switch (target.getStatus()) {
            case AVAILABLE -> "Сейчас пользователь доступен для общения";

            case TEXT_ONLY -> "Можно написать, но звонки сейчас нежелательны";

            case DO_NOT_DISTURB -> "Пользователь бодрствует, но просит не беспокоить";
        };

        for (String subscriberFID : subscribersFIDs) {
            try {
                notificationService.sendPushNotification(
                        subscriberFID,
                        title,
                        body
                );
            } catch (PushNotificationException exception) {
                log.error(
                        "Failed to send wake notification to FID {}",
                        subscriberFID,
                        exception
                );
            }
        }
    }
}
