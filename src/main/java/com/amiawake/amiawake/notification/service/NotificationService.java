package com.amiawake.amiawake.notification.service;

import com.amiawake.amiawake.common.exception.PushNotificationException;
import com.amiawake.amiawake.common.exception.PushRegistrationUnregisteredException;
import com.amiawake.amiawake.deviceregistrations.service.DeviceRegistrationService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final FirebaseMessaging firebaseMessaging;
    private final DeviceRegistrationService deviceRegistrationService;

    public NotificationService(FirebaseMessaging firebaseMessaging, DeviceRegistrationService deviceRegistrationService) {
        this.firebaseMessaging = firebaseMessaging;
        this.deviceRegistrationService = deviceRegistrationService;
    }

    public String sendPushNotification(
            String firebaseInstallationId,
            String title,
            String body
    ) {
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setNotification(notification)
                .setFid(firebaseInstallationId)
                .build();

        try {
            return firebaseMessaging.send(message);
        } catch (FirebaseMessagingException exception) {
            MessagingErrorCode errorCode = exception.getMessagingErrorCode();

            if (errorCode == MessagingErrorCode.UNREGISTERED) {
                throw new PushRegistrationUnregisteredException(exception.getMessage(), exception);
            }

            throw new PushNotificationException(exception.getMessage(), exception);
        }
    }
}
