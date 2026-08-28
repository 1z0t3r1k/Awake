package com.amiawake.amiawake.user.dto;

import jakarta.validation.constraints.NotBlank;

public record TimeZoneRequest(@NotBlank String zoneId) {
}
