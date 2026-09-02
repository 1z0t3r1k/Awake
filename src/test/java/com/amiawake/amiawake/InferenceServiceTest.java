package com.amiawake.amiawake;

import com.amiawake.amiawake.inference.model.GoogleSleepFeature;
import com.amiawake.amiawake.inference.model.InferenceResult;
import com.amiawake.amiawake.inference.model.UserFeatures;
import com.amiawake.amiawake.inference.service.InferenceService;
import com.amiawake.amiawake.inference.states.ChargingState;
import com.amiawake.amiawake.inference.states.ScheduleState;
import com.amiawake.amiawake.inference.states.ScreenState;
import com.amiawake.amiawake.inference.states.SleepState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("InferenceService")
class InferenceServiceTest {

    private final InferenceService inferenceService = new InferenceService();

    @Nested
    @DisplayName("Input validation")
    class InputValidation {

        @Test
        @DisplayName("should reject null features")
        void shouldRejectNullFeatures() {
            assertThatNullPointerException()
                    .isThrownBy(() -> inferenceService.infer(null))
                    .withMessage("Features must not be null");
        }
    }

    @Nested
    @DisplayName("Heartbeat")
    class Heartbeat {

        @Test
        @DisplayName("should return UNKNOWN when heartbeat is missing")
        void shouldReturnUnknownWhenHeartbeatIsMissing() {
            UserFeatures features = features()
                    .heartbeatMinutes(null)
                    .build();

            assertResult(features, SleepState.UNKNOWN, 0.0);
        }

        @Test
        @DisplayName("should return UNKNOWN when heartbeat is stale")
        void shouldReturnUnknownWhenHeartbeatIsStale() {
            UserFeatures features = features()
                    .heartbeatMinutes(21L)
                    .lastUnlockMinutes(1L)
                    .build();

            assertResult(features, SleepState.UNKNOWN, 0.0);
        }

        @Test
        @DisplayName("heartbeat exactly at freshness limit should still be accepted")
        void heartbeatExactlyAtLimitShouldBeAccepted() {
            UserFeatures features = features()
                    .heartbeatMinutes(20L)
                    .lastUnlockMinutes(3L)
                    .build();

            assertResult(features, SleepState.AWAKE, 0.98);
        }
    }

    @Nested
    @DisplayName("Awake detection")
    class AwakeDetection {

        @Test
        @DisplayName("should return AWAKE for very recent unlock")
        void shouldReturnAwakeForVeryRecentUnlock() {
            UserFeatures features = features()
                    .lastUnlockMinutes(3L)
                    .build();

            assertResult(features, SleepState.AWAKE, 0.98);
        }

        @Test
        @DisplayName("very recent unlock should override strong Google sleep signal")
        void shouldReturnAwakeWhenVeryRecentUnlockEvenIfGoogleIndicatesSleep() {
            UserFeatures features = features()
                    .lastUnlockMinutes(3L)
                    .screenState(ScreenState.OFF)
                    .screenOffMinutes(70L)
                    .lastMotionMinutes(40L)
                    .googleSleep(96, 2)
                    .build();

            assertResult(features, SleepState.AWAKE, 0.98);
        }

        @Test
        @DisplayName("should return AWAKE after multiple recent unlocks")
        void shouldReturnAwakeAfterMultipleRecentUnlocks() {
            UserFeatures features = features()
                    .lastUnlockMinutes(12L)
                    .unlocksLast30Minutes(2)
                    .build();

            assertResult(features, SleepState.AWAKE, 0.90);
        }

        @Test
        @DisplayName("should return AWAKE when screen is on and unlock was recent")
        void shouldReturnAwakeWhenScreenOnAndUnlockRecent() {
            UserFeatures features = features()
                    .lastUnlockMinutes(10L)
                    .unlocksLast30Minutes(1)
                    .screenState(ScreenState.ON)
                    .build();

            assertResult(features, SleepState.AWAKE, 0.90);
        }

        @Test
        @DisplayName("should return AWAKE when screen is on and motion is very recent")
        void shouldReturnAwakeWhenScreenOnAndMotionRecent() {
            UserFeatures features = features()
                    .lastUnlockMinutes(30L)
                    .screenState(ScreenState.ON)
                    .lastMotionMinutes(3L)
                    .build();

            assertResult(features, SleepState.AWAKE, 0.80);
        }

        @Test
        @DisplayName("should return AWAKE when screen is on and motion events are frequent")
        void shouldReturnAwakeWhenScreenOnAndMotionFrequent() {
            UserFeatures features = features()
                    .lastUnlockMinutes(30L)
                    .screenState(ScreenState.ON)
                    .lastMotionMinutes(20L)
                    .motionEventsLast30Minutes(3)
                    .build();

            assertResult(features, SleepState.AWAKE, 0.80);
        }
    }

    @Nested
    @DisplayName("Unknown state")
    class UnknownState {

        @Test
        @DisplayName("should return UNKNOWN when screen state is unknown")
        void shouldReturnUnknownWhenScreenStateUnknown() {
            UserFeatures features = features()
                    .screenState(ScreenState.UNKNOWN)
                    .build();

            assertResult(features, SleepState.UNKNOWN, 0.15);
        }

        @Test
        @DisplayName("should return UNKNOWN when screen is on without enough awake evidence")
        void shouldReturnUnknownWhenScreenOnWithoutEnoughEvidence() {
            UserFeatures features = features()
                    .lastUnlockMinutes(30L)
                    .screenState(ScreenState.ON)
                    .lastMotionMinutes(20L)
                    .motionEventsLast30Minutes(1)
                    .build();

            assertResult(features, SleepState.UNKNOWN, 0.40);
        }

        @Test
        @DisplayName("should return UNKNOWN when screen-off duration is missing")
        void shouldReturnUnknownWhenScreenOffDurationMissing() {
            UserFeatures features = features()
                    .screenState(ScreenState.OFF)
                    .screenOffMinutes(null)
                    .build();

            assertResult(features, SleepState.UNKNOWN, 0.20);
        }

        @Test
        @DisplayName("should return UNKNOWN when last unlock is missing")
        void shouldReturnUnknownWhenLastUnlockMissing() {
            UserFeatures features = features()
                    .screenState(ScreenState.OFF)
                    .lastUnlockMinutes(null)
                    .screenOffMinutes(60L)
                    .build();

            assertResult(features, SleepState.UNKNOWN, 0.25);
        }

        @Test
        @DisplayName("recent motion should prevent sleeping classification")
        void recentMotionShouldPreventSleepingClassification() {
            UserFeatures features = features()
                    .lastUnlockMinutes(90L)
                    .screenState(ScreenState.OFF)
                    .screenOffMinutes(90L)
                    .lastMotionMinutes(5L)
                    .googleSleep(95, 2)
                    .build();

            assertResult(features, SleepState.UNKNOWN, 0.50);
        }
    }

    @Nested
    @DisplayName("Scheduled sleep")
    class ScheduledSleep {

        @Test
        @DisplayName("should detect scheduled sleep from screen and unlock inactivity")
        void shouldDetectScheduledSleep() {
            UserFeatures features = features()
                    .lastUnlockMinutes(50L)
                    .screenOffMinutes(50L)
                    .lastMotionMinutes(20L)
                    .scheduleState(ScheduleState.IN_SLEEP_WINDOW)
                    .build();

            assertResult(features, SleepState.SLEEPING, 0.78);
        }

        @Test
        @DisplayName("low motion should increase scheduled sleep confidence")
        void lowMotionShouldIncreaseScheduledSleepConfidence() {
            UserFeatures features = features()
                    .lastUnlockMinutes(50L)
                    .screenOffMinutes(50L)
                    .lastMotionMinutes(40L)
                    .scheduleState(ScheduleState.IN_SLEEP_WINDOW)
                    .build();

            assertResult(features, SleepState.SLEEPING, 0.85);
        }

        @Test
        @DisplayName("long charging should increase scheduled sleep confidence")
        void chargingShouldIncreaseScheduledSleepConfidence() {
            UserFeatures features = features()
                    .lastUnlockMinutes(50L)
                    .screenOffMinutes(50L)
                    .lastMotionMinutes(20L)
                    .chargingState(ChargingState.CHARGING)
                    .chargingDurationMinutes(40L)
                    .scheduleState(ScheduleState.IN_SLEEP_WINDOW)
                    .build();

            assertResult(features, SleepState.SLEEPING, 0.83);
        }

        @Test
        @DisplayName("Google sleep should increase scheduled sleep confidence")
        void googleShouldIncreaseScheduledSleepConfidence() {
            UserFeatures features = features()
                    .lastUnlockMinutes(50L)
                    .screenOffMinutes(50L)
                    .lastMotionMinutes(20L)
                    .scheduleState(ScheduleState.IN_SLEEP_WINDOW)
                    .googleSleep(90, 5)
                    .build();

            assertResult(features, SleepState.SLEEPING, 0.83);
        }

        @Test
        @DisplayName("scheduled sleep confidence should respect upper cap")
        void scheduledSleepConfidenceShouldRespectUpperCap() {
            UserFeatures features = features()
                    .lastUnlockMinutes(60L)
                    .screenOffMinutes(60L)
                    .lastMotionMinutes(40L)
                    .chargingState(ChargingState.CHARGING)
                    .chargingDurationMinutes(40L)
                    .scheduleState(ScheduleState.IN_SLEEP_WINDOW)
                    .googleSleep(95, 5)
                    .build();

            assertResult(features, SleepState.SLEEPING, 0.95);
        }
    }

    @Nested
    @DisplayName("Google Sleep API")
    class GoogleSleepApi {

        @Test
        @DisplayName("should detect sleep outside schedule when fresh Google signal is strongly supported")
        void shouldReturnSleepingWhenGoogleSleepIsFreshAndOtherSignalsSupportSleep() {
            UserFeatures features = features()
                    .lastUnlockMinutes(75L)
                    .screenOffMinutes(70L)
                    .lastMotionMinutes(40L)
                    .scheduleState(ScheduleState.OUTSIDE_SLEEP_WINDOW)
                    .googleSleep(92, 5)
                    .build();

            assertResult(features, SleepState.SLEEPING, 0.82);
        }

        @Test
        @DisplayName("should ignore stale Google sleep classification")
        void shouldIgnoreStaleGoogleSleepClassification() {
            UserFeatures features = features()
                    .lastUnlockMinutes(75L)
                    .screenOffMinutes(70L)
                    .lastMotionMinutes(40L)
                    .scheduleState(ScheduleState.OUTSIDE_SLEEP_WINDOW)
                    .googleSleep(95, 21)
                    .build();

            assertResult(features, SleepState.UNKNOWN, 0.40);
        }

        @Test
        @DisplayName("should ignore Google sleep confidence below strong threshold")
        void shouldIgnoreWeakGoogleSleepConfidence() {
            UserFeatures features = features()
                    .lastUnlockMinutes(75L)
                    .screenOffMinutes(70L)
                    .lastMotionMinutes(40L)
                    .scheduleState(ScheduleState.OUTSIDE_SLEEP_WINDOW)
                    .googleSleep(84, 5)
                    .build();

            assertResult(features, SleepState.UNKNOWN, 0.40);
        }

        @Test
        @DisplayName("Google threshold value should be treated as strong signal")
        void googleConfidenceExactlyAtThresholdShouldBeStrong() {
            UserFeatures features = features()
                    .lastUnlockMinutes(75L)
                    .screenOffMinutes(70L)
                    .lastMotionMinutes(40L)
                    .googleSleep(85, 5)
                    .build();

            assertResult(features, SleepState.SLEEPING, 0.82);
        }

        @Test
        @DisplayName("Google classification exactly at freshness limit should be accepted")
        void googleClassificationExactlyAtFreshnessLimitShouldBeAccepted() {
            UserFeatures features = features()
                    .lastUnlockMinutes(75L)
                    .screenOffMinutes(70L)
                    .lastMotionMinutes(40L)
                    .googleSleep(95, 20)
                    .build();

            assertResult(features, SleepState.SLEEPING, 0.82);
        }

        @Test
        @DisplayName("strong Google signal alone should not be enough for sleep")
        void strongGoogleSignalAloneShouldNotBeEnough() {
            UserFeatures features = features()
                    .lastUnlockMinutes(30L)
                    .screenOffMinutes(20L)
                    .lastMotionMinutes(20L)
                    .googleSleep(99, 1)
                    .build();

            assertResult(features, SleepState.UNKNOWN, 0.40);
        }
    }

    @Nested
    @DisplayName("Unscheduled sleep fallback")
    class UnscheduledSleep {

        @Test
        @DisplayName("should detect unscheduled sleep after long inactivity")
        void shouldDetectUnscheduledSleepAfterLongInactivity() {
            UserFeatures features = features()
                    .lastUnlockMinutes(130L)
                    .screenOffMinutes(130L)
                    .lastMotionMinutes(40L)
                    .scheduleState(ScheduleState.OUTSIDE_SLEEP_WINDOW)
                    .build();

            assertResult(features, SleepState.SLEEPING, 0.68);
        }

        @Test
        @DisplayName("charging should increase unscheduled sleep confidence")
        void chargingShouldIncreaseUnscheduledSleepConfidence() {
            UserFeatures features = features()
                    .lastUnlockMinutes(130L)
                    .screenOffMinutes(130L)
                    .lastMotionMinutes(40L)
                    .chargingState(ChargingState.CHARGING)
                    .chargingDurationMinutes(40L)
                    .scheduleState(ScheduleState.OUTSIDE_SLEEP_WINDOW)
                    .build();

            assertResult(features, SleepState.SLEEPING, 0.75);
        }

        @Test
        @DisplayName("119 minutes of inactivity should not trigger unscheduled sleep")
        void shouldNotTriggerUnscheduledSleepBeforeThreshold() {
            UserFeatures features = features()
                    .lastUnlockMinutes(119L)
                    .screenOffMinutes(119L)
                    .lastMotionMinutes(40L)
                    .scheduleState(ScheduleState.OUTSIDE_SLEEP_WINDOW)
                    .build();

            assertResult(features, SleepState.UNKNOWN, 0.40);
        }

        @Test
        @DisplayName("120 minutes of inactivity should trigger unscheduled sleep")
        void shouldTriggerUnscheduledSleepAtThreshold() {
            UserFeatures features = features()
                    .lastUnlockMinutes(120L)
                    .screenOffMinutes(120L)
                    .lastMotionMinutes(40L)
                    .scheduleState(ScheduleState.OUTSIDE_SLEEP_WINDOW)
                    .build();

            assertResult(features, SleepState.SLEEPING, 0.68);
        }
    }

    private void assertResult(
            UserFeatures features,
            SleepState expectedState,
            double expectedConfidence
    ) {
        InferenceResult result = inferenceService.infer(features);

        assertThat(result.state())
                .isEqualTo(expectedState);

        assertThat(result.confidence())
                .isCloseTo(
                        expectedConfidence,
                        org.assertj.core.data.Offset.offset(0.0001)
                );
    }

    private TestFeaturesBuilder features() {
        return new TestFeaturesBuilder();
    }

    private static class TestFeaturesBuilder {

        /*
         * Defaults are deliberately neutral:
         *
         * - heartbeat is fresh
         * - no recent unlock
         * - screen is OFF
         * - inactivity is not long enough to infer sleep
         * - motion is neither recent nor "low"
         * - not charging
         * - outside sleep schedule
         * - no Google signal
         *
         * Therefore the default result should eventually be UNKNOWN.
         */

        private Long lastUnlockMinutes = 60L;
        private long unlocksLast30Minutes = 0;

        private ScreenState screenState = ScreenState.OFF;
        private Long screenOffMinutes = 20L;

        private Long lastMotionMinutes = 20L;
        private long motionEventsLast30Minutes = 0;

        private ChargingState chargingState = ChargingState.NOT_CHARGING;
        private Long chargingDurationMinutes = null;

        private ScheduleState scheduleState =
                ScheduleState.OUTSIDE_SLEEP_WINDOW;

        private Long heartbeatMinutes = 1L;

        private GoogleSleepFeature googleSleepFeature = null;

        TestFeaturesBuilder lastUnlockMinutes(Long value) {
            this.lastUnlockMinutes = value;
            return this;
        }

        TestFeaturesBuilder unlocksLast30Minutes(long value) {
            this.unlocksLast30Minutes = value;
            return this;
        }

        TestFeaturesBuilder screenState(ScreenState value) {
            this.screenState = value;
            return this;
        }

        TestFeaturesBuilder screenOffMinutes(Long value) {
            this.screenOffMinutes = value;
            return this;
        }

        TestFeaturesBuilder lastMotionMinutes(Long value) {
            this.lastMotionMinutes = value;
            return this;
        }

        TestFeaturesBuilder motionEventsLast30Minutes(long value) {
            this.motionEventsLast30Minutes = value;
            return this;
        }

        TestFeaturesBuilder chargingState(ChargingState value) {
            this.chargingState = value;
            return this;
        }

        TestFeaturesBuilder chargingDurationMinutes(Long value) {
            this.chargingDurationMinutes = value;
            return this;
        }

        TestFeaturesBuilder scheduleState(ScheduleState value) {
            this.scheduleState = value;
            return this;
        }

        TestFeaturesBuilder heartbeatMinutes(Long value) {
            this.heartbeatMinutes = value;
            return this;
        }

        TestFeaturesBuilder googleSleep(
                int confidence,
                long ageMinutes
        ) {
            this.googleSleepFeature =
                    new GoogleSleepFeature(confidence, ageMinutes);

            return this;
        }

        UserFeatures build() {
            return new UserFeatures(
                    Optional.ofNullable(lastUnlockMinutes),
                    unlocksLast30Minutes,

                    screenState,
                    Optional.ofNullable(screenOffMinutes),

                    Optional.ofNullable(lastMotionMinutes),
                    motionEventsLast30Minutes,

                    chargingState,
                    Optional.ofNullable(chargingDurationMinutes),

                    scheduleState,

                    Optional.ofNullable(heartbeatMinutes),

                    Optional.ofNullable(googleSleepFeature)
            );
        }
    }
}