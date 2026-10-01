package com.amiawake.amiawake.inference.service;

import com.amiawake.amiawake.inference.model.GoogleSleepFeature;
import com.amiawake.amiawake.inference.model.InferenceResult;
import com.amiawake.amiawake.inference.model.UserFeatures;
import com.amiawake.amiawake.inference.states.ChargingState;
import com.amiawake.amiawake.inference.states.ScheduleState;
import com.amiawake.amiawake.inference.states.ScreenState;
import com.amiawake.amiawake.inference.states.SleepState;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class InferenceService {
    private static final InferenceResult UNKNOWN = new InferenceResult(SleepState.UNKNOWN, 0.0);
    private static final long MAX_HEARTBEAT_AGE_MINUTES = 20;
    private static final long MAX_GOOGLE_SLEEP_AGE_MINUTES = 20;
    private static final long SUPPORTED_SLEEP_MINUTES = 45;
    private static final long UNSCHEDULED_SLEEP_MINUTES = 120;
    private static final long MAX_KNOWN_MOTION_AGE_MINUTES = 12 * 60;

    public InferenceResult infer(UserFeatures f) {
        Objects.requireNonNull(f, "Features must not be null");
        if (!within(f.minutesSinceLastHeartbeat(), MAX_HEARTBEAT_AGE_MINUTES))
            return UNKNOWN;
        if (hasNegativeAge(f))
            return UNKNOWN;

        if (within(f.minutesSinceLastUnlock(), 5))
            return new InferenceResult(SleepState.AWAKE, 0.98);
        if (within(f.minutesSinceLastUnlock(), 15)
                && (f.unlocksLast30Minutes() >= 2 || f.screenState() == ScreenState.ON)) {
            return new InferenceResult(SleepState.AWAKE, 0.90);
        }

        if (f.screenState() != ScreenState.OFF)
            return UNKNOWN;
        if (!atLeast(f.screenOffDurationMinutes(), SUPPORTED_SLEEP_MINUTES)
                || !atLeast(f.minutesSinceLastUnlock(), SUPPORTED_SLEEP_MINUTES))
            return UNKNOWN;
        if (within(f.minutesSinceLastMotion(), 30) || f.motionEventsLast30Minutes() > 0
                || f.unlocksLast30Minutes() > 0)
            return UNKNOWN;

        Optional<GoogleSleepFeature> google = f.googleSleepFeature().filter(g ->
                g.minutesSinceLastGoogleSleepClassification() >= 0
                        && g.minutesSinceLastGoogleSleepClassification() <= MAX_GOOGLE_SLEEP_AGE_MINUTES
                        && g.googleSleepConfidence() >= 0 && g.googleSleepConfidence() <= 100);

        if (google.filter(g -> g.googleSleepConfidence() <= 20).isPresent())
            return UNKNOWN;
        boolean strongGoogle = google.filter(g -> g.googleSleepConfidence() >= 85).isPresent();

        boolean knownInactivity = f.minutesSinceLastMotion()
                .filter(m -> m > 30 && m <= MAX_KNOWN_MOTION_AGE_MINUTES)
                .isPresent();
        boolean scheduled = f.scheduleState() == ScheduleState.IN_SLEEP_WINDOW;
        boolean charging = f.chargingState() == ChargingState.CHARGING && atLeast(f.chargingDurationMinutes(), 30);

        if (strongGoogle || (scheduled && knownInactivity)) {
            double confidence = strongGoogle ? 0.82 : 0.75;
            if (strongGoogle && scheduled)
                confidence += 0.03;
            if (charging)
                confidence += 0.03;
            return new InferenceResult(SleepState.SLEEPING, confidence);
        }

        boolean strongPassiveInactivity =
                atLeast(f.screenOffDurationMinutes(), UNSCHEDULED_SLEEP_MINUTES)
                        && atLeast(f.minutesSinceLastUnlock(), UNSCHEDULED_SLEEP_MINUTES)
                        && f.minutesSinceLastMotion()
                        .filter(m -> m >= UNSCHEDULED_SLEEP_MINUTES
                                && m <= MAX_KNOWN_MOTION_AGE_MINUTES)
                        .isPresent();

        if (strongPassiveInactivity) {
            return new InferenceResult(SleepState.SLEEPING, charging ? 0.70 : 0.65);
        }

        return UNKNOWN;
    }

    private boolean within(Optional<Long> age, long limit) {
        return age.filter(m -> m >= 0 && m <= limit).isPresent();
    }

    private boolean atLeast(Optional<Long> age, long minimum) {
        return age.filter(m -> m >= minimum).isPresent();
    }

    private boolean hasNegativeAge(UserFeatures f) {
        return java.util.stream.Stream.of(
                        f.minutesSinceLastUnlock(), f.minutesSinceLastMotion(),
                        f.screenOffDurationMinutes(), f.chargingDurationMinutes()
                )
                .anyMatch(age -> age.filter(m -> m < 0).isPresent());
    }
}
