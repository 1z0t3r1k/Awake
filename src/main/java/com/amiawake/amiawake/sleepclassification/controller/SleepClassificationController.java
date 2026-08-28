package com.amiawake.amiawake.sleepclassification.controller;

import com.amiawake.amiawake.sleepclassification.dto.SleepClassificationRequest;
import com.amiawake.amiawake.sleepclassification.service.SleepClassificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sleep-classifications")
public class SleepClassificationController {
    private final SleepClassificationService sleepClassificationService;

    public SleepClassificationController(SleepClassificationService sleepClassificationService) {
        this.sleepClassificationService = sleepClassificationService;
    }

    private UUID getIdByAuthentication(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void receiveSleepClassification(Authentication authentication, @RequestBody @Valid SleepClassificationRequest request) {
        UUID userId = getIdByAuthentication(authentication);

        sleepClassificationService.receiveClassification(userId, request);
    }
}
