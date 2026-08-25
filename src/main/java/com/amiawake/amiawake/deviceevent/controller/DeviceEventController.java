package com.amiawake.amiawake.deviceevent.controller;

import com.amiawake.amiawake.deviceevent.dto.DeviceEventBatchRequest;
import com.amiawake.amiawake.deviceevent.dto.DeviceEventRequest;
import com.amiawake.amiawake.deviceevent.service.DeviceEventService;
import jakarta.servlet.annotation.HttpConstraint;
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

    public DeviceEventController(DeviceEventService deviceEventService) {
        this.deviceEventService = deviceEventService;
    }

    private UUID getIdByAuthentication(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<Void> receiveEvent(
            Authentication authentication,
            @RequestBody @Valid DeviceEventRequest request
    ) {
        UUID userId = getIdByAuthentication(authentication);

        boolean inserted = deviceEventService.receiveEvent(request.eventId(), userId, request.type(), request.occurredAt());

        if (inserted) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void batchEvents(@RequestBody @Valid DeviceEventBatchRequest request, Authentication authentication) {
        UUID userId = getIdByAuthentication(authentication);

        deviceEventService.receiveBatch(userId, request);
    }
}
