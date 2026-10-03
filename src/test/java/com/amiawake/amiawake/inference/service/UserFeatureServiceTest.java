package com.amiawake.amiawake.inference.service;

import com.amiawake.amiawake.deviceevent.entity.*;
import com.amiawake.amiawake.deviceevent.repository.DeviceEventRepository;
import com.amiawake.amiawake.inference.states.*;
import com.amiawake.amiawake.sleepclassification.entity.SleepClassificationEvent;
import com.amiawake.amiawake.sleepclassification.repository.SleepClassificationRepository;
import com.amiawake.amiawake.sleepschedule.entity.SleepSchedule;
import com.amiawake.amiawake.sleepschedule.repository.SleepScheduleRepository;
import com.amiawake.amiawake.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserFeatureServiceTest {
    private final Instant now = Instant.parse("2026-01-01T22:00:00Z");
    private final User user = new User(UUID.randomUUID(), "test", "Test", "hash", "Europe/Moscow");
    private final DeviceEventRepository events = mock(DeviceEventRepository.class);
    private final SleepScheduleRepository schedules = mock(SleepScheduleRepository.class);
    private final SleepClassificationRepository google = mock(SleepClassificationRepository.class);
    private final UserFeatureService service = new UserFeatureService(events, schedules, google,
            Clock.fixed(now, ZoneOffset.UTC));

    private DeviceEvent event(DeviceEventType type, long minutesAgo) {
        DeviceEvent e = new DeviceEvent(UUID.randomUUID(), user, type, now.minusSeconds(minutesAgo * 60));
        ReflectionTestUtils.setField(e, "receivedAt", now);
        return e;
    }

    @Test
    void sourceEventsAreReadOnceAndDurationsAreNotSwapped() {
        when(events.findLatestForInference(user.getId(), now)).thenReturn(List.of(
                event(DeviceEventType.SCREEN_OFF, 60), event(DeviceEventType.MOTION, 40),
                event(DeviceEventType.CHARGING_STARTED, 90)));
        var f = service.buildFeatures(user);
        assertThat(f.screenState()).isEqualTo(ScreenState.OFF);
        assertThat(f.screenOffDurationMinutes()).contains(60L);
        assertThat(f.minutesSinceLastMotion()).contains(40L);
        assertThat(f.chargingState()).isEqualTo(ChargingState.CHARGING);
        assertThat(f.chargingDurationMinutes()).contains(90L);
        verify(events, times(1)).findLatestForInference(user.getId(), now);
        verify(events).countForInference(user, DeviceEventType.PHONE_UNLOCKED, now.minusSeconds(1800), now);
    }

    @Test
    void futureScreenOffCannotSetStateOrDuration() {
        when(events.findLatestForInference(user.getId(), now)).thenReturn(List.of(
                event(DeviceEventType.SCREEN_OFF, -1)));
        var f = service.buildFeatures(user);
        assertThat(f.screenState()).isEqualTo(ScreenState.UNKNOWN);
        assertThat(f.screenOffDurationMinutes()).isEmpty();
    }

    @Test
    void futureChargingStartedCannotSetStateOrDuration() {
        when(events.findLatestForInference(user.getId(), now)).thenReturn(List.of(
                event(DeviceEventType.CHARGING_STARTED, -1)));
        var f = service.buildFeatures(user);
        assertThat(f.chargingState()).isEqualTo(ChargingState.UNKNOWN);
        assertThat(f.chargingDurationMinutes()).isEmpty();
    }

    @Test
    void futurePointEventsCannotProduceNegativeAges() {
        when(events.findLatestForInference(user.getId(), now)).thenReturn(List.of(
                event(DeviceEventType.PHONE_UNLOCKED, -1),
                event(DeviceEventType.MOTION, -1),
                event(DeviceEventType.HEARTBEAT, -1)));
        var f = service.buildFeatures(user);
        assertThat(f.minutesSinceLastUnlock()).isEmpty();
        assertThat(f.minutesSinceLastMotion()).isEmpty();
        assertThat(f.minutesSinceLastHeartbeat()).isEmpty();
    }

    @Test
    void futureAtReceiptRemainsInvalidAfterTimePasses() {
        DeviceEvent e = event(DeviceEventType.SCREEN_OFF, 30);
        ReflectionTestUtils.setField(e, "receivedAt", now.minusSeconds(3600));
        when(events.findLatestForInference(user.getId(), now)).thenReturn(List.of(e));
        assertThat(service.buildFeatures(user).screenState()).isEqualTo(ScreenState.UNKNOWN);
    }

    @Test
    void staleAndConflictingEdgesAreUnknown() {
        when(events.findLatestForInference(user.getId(), now)).thenReturn(List.of(
                event(DeviceEventType.SCREEN_OFF, 721), event(DeviceEventType.CHARGING_STARTED, 50),
                event(DeviceEventType.CHARGING_STOPPED, 50)));
        var f = service.buildFeatures(user);
        assertThat(f.screenState()).isEqualTo(ScreenState.UNKNOWN);
        assertThat(f.chargingState()).isEqualTo(ChargingState.UNKNOWN);
        assertThat(f.screenOffDurationMinutes()).isEmpty();
        assertThat(f.chargingDurationMinutes()).isEmpty();
    }

    @Test
    void newestOccurrenceWinsRegardlessOfArrivalOrder() {
        when(events.findLatestForInference(user.getId(), now)).thenReturn(List.of(
                event(DeviceEventType.SCREEN_OFF, 20), event(DeviceEventType.SCREEN_ON, 50)));
        assertThat(service.buildFeatures(user).screenState()).isEqualTo(ScreenState.OFF);
    }

    @Test
    void futureGoogleIsRejected() {
        when(google.findRecentForInference(user.getId(), now.minusSeconds(3600), now)).thenReturn(List.of(
                new SleepClassificationEvent(user, now.plusSeconds(60), 99, 1, 1)));
        assertThat(service.buildFeatures(user).googleSleepFeature()).isEmpty();
    }

    @Test
    void scheduleUsesUserTimezoneAndHandlesInvalidZone() {
        when(schedules.findByUser(user)).thenReturn(Optional.of(new SleepSchedule(user,
                LocalTime.of(23, 0), LocalTime.of(7, 0))));
        assertThat(service.getScheduleState(user)).isEqualTo(ScheduleState.IN_SLEEP_WINDOW);
        ReflectionTestUtils.setField(user, "timeZone", "invalid/timezone");
        assertThat(service.getScheduleState(user)).isEqualTo(ScheduleState.UNKNOWN);
    }

    @Test
    void scheduleBoundariesAreStartInclusiveEndExclusiveAndDisabledIsUnknown() {
        var schedule = new SleepSchedule(user, LocalTime.of(23, 0), LocalTime.of(7, 0));
        assertThat(schedule.isSleepingAt(LocalTime.of(23, 0))).isTrue();
        assertThat(schedule.isSleepingAt(LocalTime.of(7, 0))).isFalse();
        schedule.changeSchedule(LocalTime.of(8, 0), LocalTime.of(16, 0));
        assertThat(schedule.isSleepingAt(LocalTime.of(8, 0))).isTrue();
        assertThat(schedule.isSleepingAt(LocalTime.of(16, 0))).isFalse();
        schedule.changeEnabled(false);
        when(schedules.findByUser(user)).thenReturn(Optional.of(schedule));
        assertThat(service.getScheduleState(user)).isEqualTo(ScheduleState.UNKNOWN);
    }

    private SleepClassificationEvent classification(int score, long minutesAgo) {
        var e = new SleepClassificationEvent(user, now.minusSeconds(minutesAgo * 60), score, 1, 1);
        ReflectionTestUtils.setField(e, "receivedAt", now);
        return e;
    }

    @Test
    void threeSpacedReadingsIgnoreSingleOutlier() {
        when(google.findRecentForInference(user.getId(), now.minusSeconds(3600), now)).thenReturn(List.of(
                classification(10, 1), classification(95, 11), classification(90, 21)));
        var f = service.buildFeatures(user).googleSleepFeature().orElseThrow();
        assertThat(f.googleSleepConfidence()).isEqualTo(90);
        assertThat(f.lowConfidenceCount()).isEqualTo(1);
        assertThat(f.sampleCount()).isEqualTo(3);
        assertThat(f.minutesSinceLastGoogleSleepClassification()).isEqualTo(1);
    }

    @Test
    void duplicateOrCloselySpacedReadingsDoNotBecomeIndependentSamples() {
        when(google.findRecentForInference(user.getId(), now.minusSeconds(3600), now)).thenReturn(List.of(
                classification(95, 1), classification(95, 1), classification(95, 2), classification(10, 6)));
        var f = service.buildFeatures(user).googleSleepFeature().orElseThrow();
        assertThat(f.sampleCount()).isEqualTo(2);
        assertThat(f.googleSleepConfidence()).isEqualTo(52);
    }

    @Test
    void twoLowReadingsRemainAContradictionAndInvalidHistoryIsIgnored() {
        var invalid = classification(99, 5);
        ReflectionTestUtils.setField(invalid, "receivedAt", now.minusSeconds(600));
        when(google.findRecentForInference(user.getId(), now.minusSeconds(3600), now)).thenReturn(List.of(
                invalid, classification(99, -1), classification(99, 61),
                classification(10, 1), classification(90, 11), classification(20, 21)));
        var f = service.buildFeatures(user).googleSleepFeature().orElseThrow();
        assertThat(f.googleSleepConfidence()).isEqualTo(20);
        assertThat(f.lowConfidenceCount()).isEqualTo(2);
        assertThat(f.sampleCount()).isEqualTo(3);
    }

    @Test
    void defaultNightUsesLocalTimezoneAndDoesNotOverrideExplicitSchedule() {
        assertThat(service.getScheduleState(user)).isEqualTo(ScheduleState.IN_DEFAULT_SLEEP_WINDOW);
        ReflectionTestUtils.setField(user, "timeZone", "UTC");
        assertThat(service.getScheduleState(user)).isEqualTo(ScheduleState.UNKNOWN);
        ReflectionTestUtils.setField(user, "timeZone", "Europe/Moscow");
        var s = new SleepSchedule(user, LocalTime.of(8, 0), LocalTime.of(16, 0));
        when(schedules.findByUser(user)).thenReturn(Optional.of(s));
        assertThat(service.getScheduleState(user)).isEqualTo(ScheduleState.OUTSIDE_SLEEP_WINDOW);
        s.changeEnabled(false);
        assertThat(service.getScheduleState(user)).isEqualTo(ScheduleState.UNKNOWN);
    }

}
