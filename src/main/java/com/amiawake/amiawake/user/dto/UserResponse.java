package com.amiawake.amiawake.user.dto;

import com.amiawake.amiawake.user.entity.AvailabilityStatus;

import java.util.UUID;

public record UserResponse(UUID id, String username, String displayName, String timeZone, AvailabilityStatus status) {
}
