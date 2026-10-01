package com.amiawake.amiawake.deviceregistrations.service;

import com.amiawake.amiawake.deviceregistrations.dto.DeviceRegistrationRequest;
import com.amiawake.amiawake.deviceregistrations.entity.DeviceRegistration;
import com.amiawake.amiawake.deviceregistrations.repository.DeviceRegistrationRepository;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
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
        Optional<DeviceRegistration> optionalDeviceRegistration = deviceRegistrationRepository.findByFirebaseInstallationId(
                request.firebaseInstallationId()
        );

        DeviceRegistration deviceRegistration;

        if (optionalDeviceRegistration.isEmpty()) {
            deviceRegistration = new DeviceRegistration(user, request.firebaseInstallationId());

            deviceRegistrationRepository.save(deviceRegistration);
        } else {
            deviceRegistration = optionalDeviceRegistration.get();

            deviceRegistration.reassignTo(user);
            deviceRegistration.refresh();
        }
    }

    public List<DeviceRegistration> getUserDeviceRegistrations(UUID userId) {
        return deviceRegistrationRepository.findAllByUserId(userService.getUserById(userId));
    }

    public List<String> getFirebaseInstallationIds(Collection<UUID> ids) {
        Objects.requireNonNull(ids, "User ids must not be null");
        if (ids.isEmpty()) {
            return List.of();
        }

        return deviceRegistrationRepository.findFirebaseInstallationIdsByUserIdIn(ids);
    }

    @Transactional
    public void deleteDeviceRegistrationByFid(String fid) {
        deviceRegistrationRepository.deleteByFirebaseInstallationId(fid);
    }
}
