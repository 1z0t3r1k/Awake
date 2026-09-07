package com.amiawake.amiawake.deviceevent.controller;

import com.amiawake.amiawake.common.security.AuthenticatedUserIdResolver;
import com.amiawake.amiawake.deviceevent.dto.DeviceEventBatchRequest;
import com.amiawake.amiawake.deviceevent.dto.DeviceEventRequest;
import com.amiawake.amiawake.deviceevent.service.DeviceEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/device-events")
public class DeviceEventController {
    private final DeviceEventService deviceEventService;
    private final AuthenticatedUserIdResolver authenticatedUserIdResolver;

    public DeviceEventController(
            DeviceEventService deviceEventService,
            AuthenticatedUserIdResolver authenticatedUserIdResolver
    ) {
        this.deviceEventService = deviceEventService;
        this.authenticatedUserIdResolver = authenticatedUserIdResolver;
    }

    @PostMapping
    public ResponseEntity<Void> receiveEvent(
            Authentication authentication,
            @RequestBody @Valid DeviceEventRequest request
    ) {
        UUID userId = authenticatedUserIdResolver.resolve(authentication);

        boolean inserted = deviceEventService.receiveEvent(request.eventId(), userId, request.type(), request.occurredAt());

        if (inserted) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void batchEvents(@RequestBody @Valid DeviceEventBatchRequest request, Authentication authentication) {
        UUID userId = authenticatedUserIdResolver.resolve(authentication);

        deviceEventService.receiveBatch(userId, request);
    }
}
