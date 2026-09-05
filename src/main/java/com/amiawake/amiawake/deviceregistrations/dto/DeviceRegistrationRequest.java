package com.amiawake.amiawake.deviceregistrations.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeviceRegistrationRequest(@NotNull UUID deviceId, @NotNull String pushToken) {
}
