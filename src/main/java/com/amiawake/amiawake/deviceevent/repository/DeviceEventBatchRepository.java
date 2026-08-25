package com.amiawake.amiawake.deviceevent.repository;

import com.amiawake.amiawake.deviceevent.entity.DeviceEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
public class DeviceEventBatchRepository {
    private final JdbcTemplate jdbcTemplate;

    public DeviceEventBatchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insertOne(
            UUID eventId,
            UUID userId,
            String type,
            Instant occurredAt,
            Instant receivedAt
    ) {
        String sql = "INSERT INTO device_events(event_id, user_id, type, occurred_at, received_at) VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";

        return jdbcTemplate.update(sql, eventId, userId, type, occurredAt, receivedAt);
    }

    public void insertBatch(List<DeviceEvent> events) {
        String sql = "INSERT INTO device_events(event_id, user_id, type, occurred_at, received_at) VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";

        jdbcTemplate.batchUpdate(
                sql,
                events,
                events.size(),
                (ps, event) -> {
                    ps.setObject(1, event.getEventId());
                    ps.setObject(2, event.getUser().getId());
                    ps.setObject(3, event.getType().name());
                    ps.setObject(4, event.getOccurredAt().atOffset(ZoneOffset.UTC));
                    ps.setObject(5, event.getReceivedAt().atOffset(ZoneOffset.UTC));
                }
        );
    }
}
