package com.amiawake.amiawake.wakesubscription.service;

import com.amiawake.amiawake.common.exception.PushNotificationException;
import com.amiawake.amiawake.common.exception.PushRegistrationUnregisteredException;
import com.amiawake.amiawake.deviceregistrations.service.DeviceRegistrationService;
import com.amiawake.amiawake.notification.service.NotificationService;
import com.amiawake.amiawake.user.projection.UserNotificationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WakeNotificationService {
    private static final Logger log =
            LoggerFactory.getLogger(WakeNotificationService.class);
    private final DeviceRegistrationService deviceRegistrationService;
    private final NotificationService notificationService;

    public WakeNotificationService(
            DeviceRegistrationService deviceRegistrationService,
            NotificationService notificationService
    ) {
        this.deviceRegistrationService = deviceRegistrationService;
        this.notificationService = notificationService;
    }

    private List<String> getWakeNotificationRecipients(
            List<UUID> subscriberIds
    ) {
        return deviceRegistrationService
                .getFirebaseInstallationIds(subscriberIds);
    }

    public void sendWakeNotifications(UserNotificationInfo target, List<UUID> subscriberIds) {
        List<String> subscriberFIDs = getWakeNotificationRecipients(subscriberIds);

        String title = target.displayName() + " — появилась активность";

        String body = switch (target.status()) {
            case AVAILABLE -> "После предполагаемого сна замечено использование телефона. Статус общения: можно звонить.";

            case TEXT_ONLY -> "После предполагаемого сна замечено использование телефона. Статус общения: лучше написать.";

            case DO_NOT_DISTURB -> "После предполагаемого сна замечено использование телефона. Пользователь просит не беспокоить.";
        };

        for (String subscriberFID : subscriberFIDs) {
            try {
                notificationService.sendPushNotification(
                        subscriberFID,
                        title,
                        body
                );
            } catch (PushRegistrationUnregisteredException exception) {
                try {
                    deviceRegistrationService.deleteDeviceRegistrationByFid(subscriberFID);

                    log.warn(
                            "FID {} is unregistered and was removed",
                            subscriberFID
                    );
                } catch (DataAccessException cleanupException) {
                    log.error(
                            "Failed to remove unregistered FID {}",
                            subscriberFID,
                            cleanupException
                    );
                }
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
