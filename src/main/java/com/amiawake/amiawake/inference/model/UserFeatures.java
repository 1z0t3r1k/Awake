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

        Optional<GoogleSleepFeature> googleSleepFeature,
        Optional<Long> minutesSinceLastScreenChange
) {
    public UserFeatures(Optional<Long> unlockAge, long unlockCount, ScreenState screen,
                        Optional<Long> screenOffAge, Optional<Long> motionAge, long motionCount,
                        ChargingState charging, Optional<Long> chargingAge, ScheduleState schedule,
                        Optional<Long> heartbeatAge, Optional<GoogleSleepFeature> google) {
        this(unlockAge, unlockCount, screen, screenOffAge, motionAge, motionCount, charging,
                chargingAge, schedule, heartbeatAge, google,
                screen == ScreenState.OFF ? screenOffAge : Optional.empty());
    }
}
