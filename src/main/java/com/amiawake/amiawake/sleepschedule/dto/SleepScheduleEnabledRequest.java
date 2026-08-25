package com.amiawake.amiawake.sleepschedule.dto;

import jakarta.validation.constraints.NotNull;

public record SleepScheduleEnabledRequest(@NotNull boolean enabled) {
}
