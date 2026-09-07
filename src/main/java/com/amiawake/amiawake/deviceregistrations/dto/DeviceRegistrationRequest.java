package com.amiawake.amiawake.deviceregistrations.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceRegistrationRequest(@NotBlank String firebaseInstallationId) {
}
