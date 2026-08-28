package com.amiawake.amiawake.inference.model;

import com.amiawake.amiawake.inference.states.ChargingState;
import com.amiawake.amiawake.inference.states.ScheduleState;
import com.amiawake.amiawake.inference.states.ScreenState;

import java.util.Optional;

public record UserFeatures(
        Optional<Long> minutesSinceLastUnlock,
        long unlocksLast30Minutes,

        ScreenState screenState,
        Optional<Long> screenOffDurationMinutes,

        Optional<Long> minutesSinceLastMotion,
        long motionEventsLast30Minutes,

        ChargingState chargingState,
        Optional<Long> chargingDurationMinutes,

        ScheduleState scheduleState,

        Optional<Long> minutesSinceLastHeartbeat,
        //        boolean hasThirtyMinuteCoverage

        Optional<GoogleSleepFeature> googleSleepFeature
) {
}