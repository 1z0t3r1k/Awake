package com.amiawake.amiawake.deviceevent.dto;

import com.amiawake.amiawake.deviceevent.entity.DeviceEventType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record DeviceEventRequest(@NotNull UUID eventId, @NotNull DeviceEventType type, @NotNull Instant occurredAt) {
}
