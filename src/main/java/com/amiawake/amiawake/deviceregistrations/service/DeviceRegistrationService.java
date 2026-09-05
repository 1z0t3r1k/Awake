package com.amiawake.amiawake.deviceregistrations.service;

import com.amiawake.amiawake.deviceregistrations.dto.DeviceRegistrationRequest;
import com.amiawake.amiawake.deviceregistrations.entity.DeviceRegistration;
import com.amiawake.amiawake.deviceregistrations.repository.DeviceRegistrationRepository;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceRegistrationService {
    private final DeviceRegistrationRepository deviceRegistrationRepository;
    private final UserService userService;

    public DeviceRegistrationService(DeviceRegistrationRepository deviceRegistrationRepository, UserService userService) {
        this.deviceRegistrationRepository = deviceRegistrationRepository;
        this.userService = userService;
    }

    @Transactional
    public void upsertDeviceRegistration(UUID userId, DeviceRegistrationRequest request) {
        User user = userService.getUserById(userId);
        Optional<DeviceRegistration> optionalDeviceRegistration = deviceRegistrationRepository.findByUserAndDeviceId(
                user,
                request.deviceId()
        );

        DeviceRegistration deviceRegistration;

        if (optionalDeviceRegistration.isEmpty()) {
            deviceRegistration = new DeviceRegistration(user, request.deviceId(), request.pushToken());

            deviceRegistrationRepository.save(deviceRegistration);
        } else {
            deviceRegistration = optionalDeviceRegistration.get();

            if (!deviceRegistration.getPushToken().equals(request.pushToken())) {
                deviceRegistration.updatePushToken(request.pushToken());
            }
        }
    }

    public List<DeviceRegistration> getUserDeviceRegistrations(User user) {
        return deviceRegistrationRepository.findAllByUser(user);
    }
}
