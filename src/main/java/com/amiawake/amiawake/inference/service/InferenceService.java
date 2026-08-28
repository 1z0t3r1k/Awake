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
    private static final long MAX_HEARTBEAT_AGE_MINUTES = 20;

    private static final long VERY_RECENT_UNLOCK_MINUTES = 5;
    private static final long RECENT_UNLOCK_MINUTES = 15;

    private static final long RECENT_MOTION_MINUTES = 10;
    private static final long LOW_MOTION_MINUTES = 30;

    private static final long SCHEDULED_SLEEP_MINUTES = 45;
    private static final long UNSCHEDULED_SLEEP_MINUTES = 120;

    private static final long LONG_CHARGING_MINUTES = 30;

    private static final long MAX_GOOGLE_SLEEP_AGE_MINUTES = 20;
    private static final int STRONG_GOOGLE_SLEEP_CONFIDENCE = 85;
    private static final long GOOGLE_SUPPORTED_SLEEP_MINUTES = 45;

    public InferenceResult infer(UserFeatures features) {
        Objects.requireNonNull(features, "Features must not be null");

        Optional<Long> minutesSinceLastHeartbeat =
                features.minutesSinceLastHeartbeat();

        if (minutesSinceLastHeartbeat.isEmpty()
                || minutesSinceLastHeartbeat.get() > MAX_HEARTBEAT_AGE_MINUTES) {

            return new InferenceResult(
                    SleepState.UNKNOWN,
                    0.0
            );
        }

        Optional<Long> minutesSinceLastUnlock =
                features.minutesSinceLastUnlock();

        if (minutesSinceLastUnlock.isPresent()
                && minutesSinceLastUnlock.get() <= VERY_RECENT_UNLOCK_MINUTES) {

            return new InferenceResult(
                    SleepState.AWAKE,
                    0.98
            );
        }

        if (minutesSinceLastUnlock.isPresent()
                && minutesSinceLastUnlock.get() <= RECENT_UNLOCK_MINUTES
                && features.unlocksLast30Minutes() >= 2) {

            return new InferenceResult(
                    SleepState.AWAKE,
                    0.90
            );
        }

        boolean strongGoogleSleep =
                features.googleSleepFeature()
                        .filter(this::isFreshGoogleClassification)
                        .map(GoogleSleepFeature::googleSleepConfidence)
                        .map(confidence ->
                                confidence >= STRONG_GOOGLE_SLEEP_CONFIDENCE)
                        .orElse(false);

        if (features.screenState() == ScreenState.UNKNOWN) {
            return new InferenceResult(
                    SleepState.UNKNOWN,
                    0.15
            );
        }

        if (features.screenState() == ScreenState.ON) {

            if (minutesSinceLastUnlock.isPresent()
                    && minutesSinceLastUnlock.get() <= RECENT_UNLOCK_MINUTES) {

                return new InferenceResult(
                        SleepState.AWAKE,
                        0.90
                );
            }

            boolean recentMotion =
                    features.minutesSinceLastMotion()
                            .map(minutes -> minutes <= 5)
                            .orElse(false);

            if (recentMotion || features.motionEventsLast30Minutes() >= 3) {
                return new InferenceResult(
                        SleepState.AWAKE,
                        0.80
                );
            }

            return new InferenceResult(
                    SleepState.UNKNOWN,
                    0.40
            );
        }

        Optional<Long> screenOffDuration =
                features.screenOffDurationMinutes();

        if (screenOffDuration.isEmpty()) {
            return new InferenceResult(
                    SleepState.UNKNOWN,
                    0.20
            );
        }

        if (minutesSinceLastUnlock.isEmpty()) {
            return new InferenceResult(
                    SleepState.UNKNOWN,
                    0.25
            );
        }

        long screenOffMinutes = screenOffDuration.get();
        long lastUnlockMinutes = minutesSinceLastUnlock.get();

        boolean recentMotion =
                features.minutesSinceLastMotion()
                        .map(minutes -> minutes <= RECENT_MOTION_MINUTES)
                        .orElse(false);

        if (recentMotion) {
            return new InferenceResult(
                    SleepState.UNKNOWN,
                    0.50
            );
        }

        boolean lowMotion =
                features.minutesSinceLastMotion()
                        .map(minutes -> minutes >= LOW_MOTION_MINUTES)
                        .orElse(false);

        boolean longCharging =
                features.chargingState() == ChargingState.CHARGING
                        && features.chargingDurationMinutes()
                        .map(minutes -> minutes >= LONG_CHARGING_MINUTES)
                        .orElse(false);

        boolean withinSleepSchedule =
                features.scheduleState()
                        == ScheduleState.IN_SLEEP_WINDOW;

        if (withinSleepSchedule
                && screenOffMinutes >= SCHEDULED_SLEEP_MINUTES
                && lastUnlockMinutes >= SCHEDULED_SLEEP_MINUTES) {

            double confidence = 0.78;

            if (lowMotion) {
                confidence += 0.07;
            }

            if (longCharging) {
                confidence += 0.05;
            }

            if (strongGoogleSleep) {
                confidence += 0.05;
            }

            return new InferenceResult(
                    SleepState.SLEEPING,
                    Math.min(confidence, 0.95)
            );
        }

        if (strongGoogleSleep
                && screenOffMinutes >= GOOGLE_SUPPORTED_SLEEP_MINUTES
                && lastUnlockMinutes >= GOOGLE_SUPPORTED_SLEEP_MINUTES
                && lowMotion) {

            return new InferenceResult(
                    SleepState.SLEEPING,
                    0.82
            );
        }

        if (screenOffMinutes >= UNSCHEDULED_SLEEP_MINUTES
                && lastUnlockMinutes >= UNSCHEDULED_SLEEP_MINUTES
                && lowMotion) {

            double confidence = 0.68;

            if (longCharging) {
                confidence += 0.07;
            }

            return new InferenceResult(
                    SleepState.SLEEPING,
                    Math.min(confidence, 0.75)
            );
        }

        return new InferenceResult(
                SleepState.UNKNOWN,
                0.40
        );
    }

    private boolean isFreshGoogleClassification(GoogleSleepFeature feature) {
        return feature.minutesSinceLastGoogleSleepClassification()
                <= MAX_GOOGLE_SLEEP_AGE_MINUTES;
    }
}