package com.amiawake.amiawake.deviceregistrations.repository;

import com.amiawake.amiawake.deviceregistrations.entity.DeviceRegistration;
import com.amiawake.amiawake.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRegistrationRepository extends JpaRepository<DeviceRegistration, UUID> {
    Optional<DeviceRegistration> findByUserAndFirebaseInstallationId(User user, String firebaseInstallationId);

    Optional<DeviceRegistration> findByFirebaseInstallationId(String firebaseInstallationId);

    List<DeviceRegistration> findAllByUserId(User user);

    @Query("""
            select dr.firebaseInstallationId
            from DeviceRegistration dr
            where dr.user.id in :ids
            """)
    List<String> findFirebaseInstallationIdsByUserIdIn(Collection<UUID> ids);

    void deleteByFirebaseInstallationId(String firebaseInstallationId);
}
