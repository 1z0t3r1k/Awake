package com.amiawake.amiawake.wakesubscription.service;

import com.amiawake.amiawake.deviceregistrations.entity.DeviceRegistration;
import com.amiawake.amiawake.deviceregistrations.service.DeviceRegistrationService;
import com.amiawake.amiawake.user.entity.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WakeNotificationService {
    private final DeviceRegistrationService deviceRegistrationService;
    private final WakeSubscriptionService wakeSubscriptionService;

    public WakeNotificationService(WakeSubscriptionService wakeSubscriptionService, DeviceRegistrationService deviceRegistrationService) {
        this.deviceRegistrationService = deviceRegistrationService;
        this.wakeSubscriptionService = wakeSubscriptionService;
    }

    public List<String> getPushTokensForWakeNotification(User target) {
        List<User> subscribers = wakeSubscriptionService.getSubscribersForTarget(target);
        List<String> pushTokens = new ArrayList<>();

        for (User subscriber : subscribers) {
            List<DeviceRegistration> deviceRegistrations = deviceRegistrationService.getUserDeviceRegistrations(subscriber);

            for (DeviceRegistration deviceRegistration : deviceRegistrations) {
                pushTokens.add(deviceRegistration.getPushToken());
            }
        }

        return pushTokens;
    }
}
