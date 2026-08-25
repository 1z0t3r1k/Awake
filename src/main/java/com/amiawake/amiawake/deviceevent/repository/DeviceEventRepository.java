package com.amiawake.amiawake.deviceevent.repository;

import com.amiawake.amiawake.deviceevent.entity.DeviceEvent;
import com.amiawake.amiawake.deviceevent.entity.DeviceEventType;
import com.amiawake.amiawake.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceEventRepository extends JpaRepository<DeviceEvent, UUID> {
    @Modifying
    @Query(value = "INSERT INTO device_events(event_id, user_id, type, occurred_at, received_at) VALUES (:eventId, :userId, :type, :occurredAt, :receivedAt) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") UUID eventId,
            @Param("userId") UUID userId,
            @Param("type") String type,
            @Param("occurredAt") Instant occurredAt,
            @Param("receivedAt") Instant receivedAt
    );

    Optional<DeviceEvent> findFirstByUserAndTypeOrderByOccurredAtDesc(
            User user,
            DeviceEventType type
    );

    long countDeviceEventsByUserAndTypeAndOccurredAtAfter(
            User user,
            DeviceEventType type,
            Instant after
    );

    Optional<DeviceEvent> findFirstByUserAndTypeInOrderByOccurredAtDesc(User user, List<DeviceEventType> type);
}
