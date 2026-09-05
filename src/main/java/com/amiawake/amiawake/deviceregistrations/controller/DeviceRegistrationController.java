package com.amiawake.amiawake.deviceregistrations.controller;

import com.amiawake.amiawake.common.security.AuthenticatedUserIdResolver;
import com.amiawake.amiawake.deviceregistrations.dto.DeviceRegistrationRequest;
import com.amiawake.amiawake.deviceregistrations.service.DeviceRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/device-registrations")
public class DeviceRegistrationController {
    private final DeviceRegistrationService deviceRegistrationService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    public DeviceRegistrationController(
            DeviceRegistrationService deviceRegistrationService,
            AuthenticatedUserIdResolver authenticatedUserIdResolver
    ) {
        this.deviceRegistrationService = deviceRegistrationService;
        this.authenticatedUserIdResolver = authenticatedUserIdResolver;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDevice(Authentication authentication, @RequestBody @Valid DeviceRegistrationRequest request) {
        UUID userId = authenticatedUserIdResolver.resolve(authentication);

        deviceRegistrationService.upsertDeviceRegistration(userId, request);
    }
}
