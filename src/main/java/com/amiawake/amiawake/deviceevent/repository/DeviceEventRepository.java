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
import java.util.UUID;

public interface DeviceEventRepository extends JpaRepository<DeviceEvent, UUID> {
    @Query(value = """
            SELECT DISTINCT ON (type) * FROM device_events
            WHERE user_id = :userId AND occurred_at <= :now AND occurred_at <= received_at
            ORDER BY type, occurred_at DESC, received_at DESC, event_id DESC
            """, nativeQuery = true)
    List<DeviceEvent> findLatestForInference(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("""
            SELECT count(e) FROM DeviceEvent e WHERE e.user = :user AND e.type = :type
            AND e.occurredAt > :after AND e.occurredAt <= :now AND e.occurredAt <= e.receivedAt
            """)
    long countForInference(@Param("user") User user, @Param("type") DeviceEventType type,
                           @Param("after") Instant after, @Param("now") Instant now);

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

}
