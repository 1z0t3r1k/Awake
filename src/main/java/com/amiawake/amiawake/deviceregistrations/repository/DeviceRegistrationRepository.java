package com.amiawake.amiawake.deviceregistrations.repository;

import com.amiawake.amiawake.deviceregistrations.entity.DeviceRegistration;
import com.amiawake.amiawake.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRegistrationRepository extends JpaRepository<DeviceRegistration, UUID> {
    Optional<DeviceRegistration> findByUserAndDeviceId(User user, UUID deviceId);

    List<DeviceRegistration> findAllByUser(User user);

    User user(User user);
}
