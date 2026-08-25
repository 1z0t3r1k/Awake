package com.amiawake.amiawake.sleepschedule.dto;

import java.time.LocalTime;

public record SleepScheduleResponse(
        LocalTime sleepTime,
        LocalTime wakeTime,
        boolean enabled
) {
}
