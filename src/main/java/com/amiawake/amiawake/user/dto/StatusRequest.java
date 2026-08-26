package com.amiawake.amiawake.user.dto;

import com.amiawake.amiawake.user.entity.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull AvailabilityStatus status) {
}
