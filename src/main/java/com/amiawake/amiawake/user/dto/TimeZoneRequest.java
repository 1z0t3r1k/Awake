package com.amiawake.amiawake.user.dto;

import jakarta.validation.constraints.NotNull;

public record TimeZoneRequest(@NotNull String zoneId) {
}
