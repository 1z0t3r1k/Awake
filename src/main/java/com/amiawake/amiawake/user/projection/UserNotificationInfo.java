package com.amiawake.amiawake.user.projection;

import com.amiawake.amiawake.user.entity.AvailabilityStatus;

public record UserNotificationInfo(String displayName, AvailabilityStatus status) {
}
