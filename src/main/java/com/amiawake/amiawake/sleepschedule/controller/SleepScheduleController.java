package com.amiawake.amiawake.sleepschedule.controller;

import com.amiawake.amiawake.sleepschedule.dto.SleepScheduleEnabledRequest;
import com.amiawake.amiawake.sleepschedule.dto.SleepScheduleRequest;
import com.amiawake.amiawake.sleepschedule.dto.SleepScheduleResponse;
import com.amiawake.amiawake.sleepschedule.service.SleepScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sleep-schedule")
public class SleepScheduleController {
    private final SleepScheduleService sleepScheduleService;

    public SleepScheduleController(SleepScheduleService sleepScheduleService) {
        this.sleepScheduleService = sleepScheduleService;
    }

    private UUID getIdByAuthentication(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    @GetMapping
    public SleepScheduleResponse getSleepSchedule(Authentication authentication) {
        UUID userId = getIdByAuthentication(authentication);

        return sleepScheduleService.getSleepSchedule(userId);
    }

    @PutMapping
    public SleepScheduleResponse setSleepSchedule(
            @Valid @RequestBody SleepScheduleRequest request,
            Authentication authentication
    ) {
        UUID userId = getIdByAuthentication(authentication);

        return sleepScheduleService.setSleepSchedule(userId, request);
    }

    @PatchMapping("/enabled")
    public SleepScheduleResponse setEnabledStatus(
            @Valid @RequestBody SleepScheduleEnabledRequest request,
            Authentication authentication
    ) {
        UUID userId = getIdByAuthentication(authentication);

        return sleepScheduleService.setEnabledStatus(userId, request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSleepSchedule(Authentication authentication) {
        UUID userId = getIdByAuthentication(authentication);

        sleepScheduleService.deleteSleepSchedule(userId);
    }
}
