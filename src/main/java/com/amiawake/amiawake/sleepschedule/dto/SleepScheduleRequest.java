package com.amiawake.amiawake.sleepschedule.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record SleepScheduleRequest(
        @NotNull LocalTime sleepTime,
        @NotNull LocalTime wakeTime
) {
}
