package com.amiawake.amiawake.notification.service;

import com.amiawake.amiawake.common.exception.PushNotificationException;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final FirebaseMessaging firebaseMessaging;

    public NotificationService(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
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
            throw new PushNotificationException(
                    "Failed to send push notification",
                    exception
            );
        }
    }
}
