package com.amiawake.amiawake.inference.service;

import com.amiawake.amiawake.inference.model.GoogleSleepFeature;
import com.amiawake.amiawake.inference.model.InferenceResult;
import com.amiawake.amiawake.inference.model.UserFeatures;
import com.amiawake.amiawake.inference.states.ChargingState;
import com.amiawake.amiawake.inference.states.ScheduleState;
import com.amiawake.amiawake.inference.states.ScreenState;
import com.amiawake.amiawake.inference.states.SleepState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class InferenceService {
    private static final Logger log = LoggerFactory.getLogger(InferenceService.class);
    private static final InferenceResult UNKNOWN = new InferenceResult(SleepState.UNKNOWN, 0.0);
    private static final long MAX_GOOGLE_AGE = 30;
    private static final long MAX_PASSIVE_COVERAGE_AGE = 8 * 60;

    public InferenceResult infer(UserFeatures f) {
        Objects.requireNonNull(f, "Features must not be null");
        if (hasNegativeAge(f)) return unknown("INVALID_TIME");

        // Direct evidence of use does not depend on a background worker's heartbeat.
        if (within(f.minutesSinceLastUnlock(), 5))
            return result(SleepState.AWAKE, 0.98, "RECENT_UNLOCK");
        if (within(f.minutesSinceLastUnlock(), 15)
                && (f.unlocksLast30Minutes() >= 2 || f.screenState() == ScreenState.ON))
            return result(SleepState.AWAKE, 0.90, "RECENT_USE");

        if (recentActivity(f)) return unknown("RECENT_ACTIVITY");
        if (f.screenState() == ScreenState.ON) return unknown("SCREEN_ON");

        Optional<GoogleSleepFeature> google = freshGoogle(f);
        if (google.filter(g -> g.googleSleepConfidence() <= 20 || g.lowConfidenceCount() >= 2).isPresent())
            return unknown("GOOGLE_CONFLICT");
        boolean strongGoogle = google.filter(g -> g.googleSleepConfidence() >= 85).isPresent();
        boolean charging = f.chargingState() == ChargingState.CHARGING
                && atLeast(f.chargingDurationMinutes(), 30);
        boolean scheduled = f.scheduleState() == ScheduleState.IN_SLEEP_WINDOW;

        // Google can still give evidence when a screen-off callback was missed.
        if (strongGoogle && quietUnlock(f, 45)
                && (f.screenState() == ScreenState.UNKNOWN || atLeast(f.screenOffDurationMinutes(), 45)))
            return result(SleepState.SLEEPING, 0.82 + (scheduled ? 0.03 : 0) + (charging ? 0.03 : 0),
                    "GOOGLE_SLEEP");

        if (f.screenState() != ScreenState.OFF) return unknown("MISSING_SCREEN_STATE");
        if (!hasPassiveCoverage(f)) return unknown("STALE_TELEMETRY");

        boolean defaultNight = f.scheduleState() == ScheduleState.IN_DEFAULT_SLEEP_WINDOW;
        long quietMinutes = defaultNight ? 90 : 60;
        if ((scheduled || defaultNight) && atLeast(f.screenOffDurationMinutes(), quietMinutes)
                && quietUnlock(f, quietMinutes)) {
            // Absence of MOTION is not a sensor reading; screen history + schedule is a weaker estimate.
            boolean knownMotion = f.minutesSinceLastMotion().filter(m -> m > 30 && m <= 720).isPresent();
            double confidence = defaultNight ? 0.60 : 0.68;
            if (knownMotion) confidence += 0.04;
            if (charging) confidence += 0.03;
            if (!within(f.minutesSinceLastHeartbeat(), 60)) confidence -= 0.08;
            return result(SleepState.SLEEPING, Math.max(0.55, confidence),
                    defaultNight ? "DEFAULT_NIGHT_ESTIMATE" : "SCHEDULE_ESTIMATE");
        }

        // Outside bedtime, keep the stricter existing fallback and require recent connectivity.
        if (within(f.minutesSinceLastHeartbeat(), 60)
                && atLeast(f.screenOffDurationMinutes(), 120) && quietUnlock(f, 120)
                && f.minutesSinceLastMotion().filter(m -> m >= 120 && m <= 720).isPresent())
            return result(SleepState.SLEEPING, charging ? 0.70 : 0.65, "LONG_INACTIVITY");
        return unknown("INSUFFICIENT_EVIDENCE");
    }

    public boolean canRetainSleep(UserFeatures f) {
        // Holding a previous estimate must not ignore use, two low readings, or a long outage.
        boolean screenCompatible = (f.screenState() == ScreenState.OFF && f.screenOffDurationMinutes().isPresent())
                || (f.screenState() == ScreenState.ON && within(f.minutesSinceLastScreenChange(), 2));
        // A notification can briefly light up a locked screen; it is not an unlock.
        return !hasNegativeAge(f) && !recentActivity(f) && screenCompatible && quietUnlock(f, 45)
                && hasPassiveCoverage(f)
                && freshGoogle(f).filter(g -> g.lowConfidenceCount() >= 2).isEmpty();
    }

    private boolean recentActivity(UserFeatures f) {
        return within(f.minutesSinceLastUnlock(), 30) || within(f.minutesSinceLastMotion(), 30)
                || f.motionEventsLast30Minutes() > 0 || f.unlocksLast30Minutes() > 0;
    }

    private boolean hasPassiveCoverage(UserFeatures f) {
        return Stream.of(f.minutesSinceLastHeartbeat(), f.screenOffDurationMinutes(),
                        f.minutesSinceLastUnlock(), f.minutesSinceLastMotion(), f.chargingDurationMinutes())
                .anyMatch(age -> within(age, MAX_PASSIVE_COVERAGE_AGE)) || freshGoogle(f).isPresent();
    }

    private Optional<GoogleSleepFeature> freshGoogle(UserFeatures f) {
        return f.googleSleepFeature().filter(g -> g.minutesSinceLastGoogleSleepClassification() >= 0
                && g.minutesSinceLastGoogleSleepClassification() <= MAX_GOOGLE_AGE
                && g.googleSleepConfidence() >= 0 && g.googleSleepConfidence() <= 100);
    }

    private boolean quietUnlock(UserFeatures f, long minimum) {
        return f.minutesSinceLastUnlock().isEmpty() || atLeast(f.minutesSinceLastUnlock(), minimum);
    }

    private boolean within(Optional<Long> age, long limit) {
        return age.filter(m -> m >= 0 && m <= limit).isPresent();
    }

    private boolean atLeast(Optional<Long> age, long minimum) {
        return age.filter(m -> m >= minimum).isPresent();
    }

    private boolean hasNegativeAge(UserFeatures f) {
        return Stream.of(f.minutesSinceLastUnlock(), f.minutesSinceLastMotion(),
                        f.screenOffDurationMinutes(), f.chargingDurationMinutes(), f.minutesSinceLastHeartbeat(), f.minutesSinceLastScreenChange())
                .anyMatch(age -> age.filter(m -> m < 0).isPresent());
    }

    private InferenceResult unknown(String reason) {
        log.debug("Sleep inference: {}", reason);
        return UNKNOWN;
    }

    private InferenceResult result(SleepState state, double confidence, String reason) {
        log.debug("Sleep inference: {} -> {} ({})", reason, state, confidence);
        return new InferenceResult(state, confidence);
    }
}
