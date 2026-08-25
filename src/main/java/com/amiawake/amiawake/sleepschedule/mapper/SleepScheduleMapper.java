package com.amiawake.amiawake.sleepschedule.mapper;

import com.amiawake.amiawake.sleepschedule.dto.SleepScheduleResponse;
import com.amiawake.amiawake.sleepschedule.entity.SleepSchedule;

public final class SleepScheduleMapper {
    private SleepScheduleMapper() {
    }

    public static SleepScheduleResponse toSleepScheduleResponse(SleepSchedule schedule) {
        return new SleepScheduleResponse(
                schedule.getSleepTime(), schedule.getWakeTime(), schedule.isEnabled()
        );
    }
}
