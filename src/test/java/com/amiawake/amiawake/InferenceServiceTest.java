package com.amiawake.amiawake;

import com.amiawake.amiawake.inference.model.*;
import com.amiawake.amiawake.inference.service.InferenceService;
import com.amiawake.amiawake.inference.states.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;

class InferenceServiceTest {
    private final InferenceService inferenceService = new InferenceService();

    @Test
    void nullAndFutureDurationsAreRejected() {
        assertThatNullPointerException().isThrownBy(() -> inferenceService.infer(null));
        assertResult(features().lastUnlockMinutes(-1L).build(), SleepState.UNKNOWN, 0);
        assertResult(features().heartbeatMinutes(-1L).lastUnlockMinutes(1L).build(), SleepState.UNKNOWN, 0);
    }

    @Test
    void freshUnlockWinsOverMissingHeartbeatAndSleepSignals() {
        assertResult(night().heartbeatMinutes(null).lastUnlockMinutes(1L).googleSleep(99, 1).build(), SleepState.AWAKE, .98);
        assertResult(night().heartbeatMinutes(600L).lastUnlockMinutes(5L).build(), SleepState.AWAKE, .98);
    }

    @Test
    void sustainedRecentUseNeedsScreenOnOrRepeatedUnlocks() {
        assertResult(features().lastUnlockMinutes(15L).screenState(ScreenState.ON).build(), SleepState.AWAKE, .90);
        assertResult(features().lastUnlockMinutes(12L).unlocksLast30Minutes(2).heartbeatMinutes(null).build(), SleepState.AWAKE, .90);
        assertResult(features().lastUnlockMinutes(6L).build(), SleepState.UNKNOWN, 0);
        assertResult(features().lastUnlockMinutes(16L).screenState(ScreenState.ON).build(), SleepState.UNKNOWN, 0);
    }

    @Test
    void bedtimeWorksWithoutMotionAndWithoutAnUnlockHistory() {
        assertResult(night().lastMotionMinutes(null).build(), SleepState.SLEEPING, .68);
        assertResult(night().lastUnlockMinutes(null).lastMotionMinutes(null).build(), SleepState.SLEEPING, .68);
    }

    @Test
    void bedtimeEstimateSurvivesDozeButHasLowerConfidence() {
        assertResult(night().lastMotionMinutes(null).heartbeatMinutes(180L).build(), SleepState.SLEEPING, .60);
        assertResult(night().lastMotionMinutes(null).heartbeatMinutes(null).build(), SleepState.SLEEPING, .60);
    }

    @Test
    void defaultNightIsWeakerAndRequiresNinetyMinutes() {
        assertResult(night().scheduleState(ScheduleState.IN_DEFAULT_SLEEP_WINDOW).screenOffMinutes(89L).build(), SleepState.UNKNOWN, 0);
        assertResult(night().scheduleState(ScheduleState.IN_DEFAULT_SLEEP_WINDOW).build(), SleepState.SLEEPING, .64);
        assertResult(night().scheduleState(ScheduleState.IN_DEFAULT_SLEEP_WINDOW).lastMotionMinutes(null).heartbeatMinutes(null).build(), SleepState.SLEEPING, .55);
    }

    @ParameterizedTest
    @ValueSource(longs = {10, 20, 30})
    void recentMotionBlocksSleepAndRetention(long age) {
        var f = night().lastMotionMinutes(age).googleSleep(99, 1).build();
        assertResult(f, SleepState.UNKNOWN, 0);
        assertThat(inferenceService.canRetainSleep(f)).isFalse();
    }

    @Test
    void screenOnAndRecentUnlockCountsBlockSleep() {
        for (var f : new UserFeatures[]{night().screenState(ScreenState.ON).build(),
                night().unlocksLast30Minutes(1).build(), night().motionEventsLast30Minutes(1).build()}) {
            assertResult(f, SleepState.UNKNOWN, 0);
            assertThat(inferenceService.canRetainSleep(f)).isFalse();
        }
    }

    @Test
    void noObservationsAndLongOutagesDoNotTurnIntoSleep() {
        assertResult(features().lastUnlockMinutes(null).lastMotionMinutes(null).screenState(ScreenState.UNKNOWN)
                .screenOffMinutes(null).heartbeatMinutes(null).scheduleState(ScheduleState.IN_SLEEP_WINDOW).build(), SleepState.UNKNOWN, 0);
        var outage = night().heartbeatMinutes(481L).lastUnlockMinutes(500L).screenOffMinutes(481L).lastMotionMinutes(null).build();
        assertResult(outage, SleepState.UNKNOWN, 0);
        assertThat(inferenceService.canRetainSleep(outage)).isFalse();
        assertResult(night().heartbeatMinutes(480L).lastUnlockMinutes(500L).screenOffMinutes(480L).lastMotionMinutes(null).build(), SleepState.SLEEPING, .60);
    }

    @Test
    void configuredBedtimeNeedsSixtyQuietMinutes() {
        assertResult(night().screenOffMinutes(59L).build(), SleepState.UNKNOWN, 0);
        assertResult(night().lastUnlockMinutes(59L).build(), SleepState.UNKNOWN, 0);
        assertResult(night().screenOffMinutes(60L).lastUnlockMinutes(60L).build(), SleepState.SLEEPING, .72);
        assertResult(night().chargingState(ChargingState.CHARGING).chargingDurationMinutes(30L).build(), SleepState.SLEEPING, .75);
    }

    @Test
    void googleDoesNotRequireHeartbeatOrMotionAndCanSurviveMissedScreenEdge() {
        assertResult(night().scheduleState(ScheduleState.UNKNOWN).lastMotionMinutes(null).heartbeatMinutes(null)
                .googleSleep(85, 30).build(), SleepState.SLEEPING, .82);
        assertResult(night().lastMotionMinutes(null).heartbeatMinutes(null).screenState(ScreenState.UNKNOWN)
                .screenOffMinutes(null).googleSleep(90, 5).build(), SleepState.SLEEPING, .85);
        assertResult(night().googleSleep(95, 5).chargingState(ChargingState.CHARGING).chargingDurationMinutes(40L).build(), SleepState.SLEEPING, .88);
    }

    @Test
    void staleInvalidOrWeakGoogleIsNotEnoughOutsideBedtime() {
        for (var f : new UserFeatures[]{night().scheduleState(ScheduleState.UNKNOWN).googleSleep(95, 31).build(),
                night().scheduleState(ScheduleState.UNKNOWN).googleSleep(84, 5).build(),
                night().scheduleState(ScheduleState.UNKNOWN).googleSleep(101, 5).build(),
                night().scheduleState(ScheduleState.UNKNOWN).googleSleep(99, -1).build()})
            assertResult(f, SleepState.UNKNOWN, 0);
    }

    @Test
    void oneLowGoogleReadingBlocksNewSleepButCanRetainPreviousSleep() {
        var f = night().googleSleep(10, 5).build();
        assertResult(f, SleepState.UNKNOWN, 0);
        assertThat(inferenceService.canRetainSleep(f)).isTrue();
        var b = night();
        b.googleSleepFeature = new GoogleSleepFeature(10, 5, 3, 2);
        assertResult(b.build(), SleepState.UNKNOWN, 0);
        assertThat(inferenceService.canRetainSleep(b.build())).isFalse();
    }

    @Test
    void unscheduledSleepRequiresCoverageAndKnownMotionInactivity() {
        assertResult(night().scheduleState(ScheduleState.UNKNOWN).screenOffMinutes(120L).lastUnlockMinutes(120L)
                .lastMotionMinutes(120L).build(), SleepState.SLEEPING, .65);
        assertResult(night().scheduleState(ScheduleState.UNKNOWN).screenOffMinutes(180L).lastUnlockMinutes(180L)
                .lastMotionMinutes(null).build(), SleepState.UNKNOWN, 0);
        assertResult(night().scheduleState(ScheduleState.UNKNOWN).screenOffMinutes(180L).lastUnlockMinutes(180L)
                .lastMotionMinutes(180L).heartbeatMinutes(61L).build(), SleepState.UNKNOWN, 0);
        assertResult(night().scheduleState(ScheduleState.UNKNOWN).screenOffMinutes(120L).lastUnlockMinutes(120L)
                .lastMotionMinutes(721L).build(), SleepState.UNKNOWN, 0);
        assertResult(night().scheduleState(ScheduleState.UNKNOWN).screenOffMinutes(119L).lastUnlockMinutes(120L)
                .lastMotionMinutes(120L).build(), SleepState.UNKNOWN, 0);
    }

    @Test
    void briefLockedScreenWakeCanRetainSleepButCannotStartNewSleep() {
        var off = night().screenOffMinutes(1L).build();
        assertResult(off, SleepState.UNKNOWN, 0);
        assertThat(inferenceService.canRetainSleep(off)).isTrue();
        for (long screenAge : new long[]{0, 2, 3}) {
            var f = new UserFeatures(Optional.of(90L), 0, ScreenState.ON, Optional.empty(),
                    Optional.empty(), 0, ChargingState.UNKNOWN, Optional.empty(),
                    ScheduleState.IN_SLEEP_WINDOW, Optional.of(5L), Optional.empty(), Optional.of(screenAge));
            assertResult(f, SleepState.UNKNOWN, 0);
            assertThat(inferenceService.canRetainSleep(f)).isEqualTo(screenAge <= 2);
        }
    }

    private TestFeaturesBuilder features() { return new TestFeaturesBuilder(); }
    private TestFeaturesBuilder night() {
        return features().screenOffMinutes(90L).lastUnlockMinutes(90L).lastMotionMinutes(60L)
                .scheduleState(ScheduleState.IN_SLEEP_WINDOW);
    }
    private void assertResult(UserFeatures features, SleepState state, double confidence) {
        var result = inferenceService.infer(features);
        assertThat(result.state()).isEqualTo(state);
        assertThat(result.confidence()).isCloseTo(confidence, within(.0001));
    }
    private static class TestFeaturesBuilder {

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
