package com.amiawake.amiawake;

import com.amiawake.amiawake.deviceevent.entity.*;
import com.amiawake.amiawake.sleepclassification.entity.SleepClassificationEvent;
import com.amiawake.amiawake.sleepclassification.repository.SleepClassificationRepository;
import com.amiawake.amiawake.deviceevent.repository.DeviceEventRepository;
import com.amiawake.amiawake.inference.model.UserFeatures;
import com.amiawake.amiawake.inference.service.UserFeatureService;
import com.amiawake.amiawake.inference.states.*;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.repository.UserRepository;
import com.amiawake.amiawake.userstate.entity.UserState;
import com.amiawake.amiawake.userstate.event.UserWokeUpEvent;
import com.amiawake.amiawake.userstate.repository.UserStateRepository;
import com.amiawake.amiawake.userstate.service.UserStateCalculationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import({TestcontainersConfiguration.class, InferenceFlowIntegrationTest.EventsConfiguration.class})
@Testcontainers(disabledWithoutDocker = true)
class InferenceFlowIntegrationTest {
    @Autowired UserRepository users;
    @Autowired UserStateRepository states;
    @Autowired DeviceEventRepository events;
    @Autowired SleepClassificationRepository classifications;
    @Autowired UserStateCalculationService calculation;
    @Autowired WakeEvents published;
    @MockitoBean UserFeatureService features;

    @TestConfiguration
    static class EventsConfiguration {
        @Bean WakeEvents wakeEvents() { return new WakeEvents(); }
    }
    static class WakeEvents {
        final Queue<UUID> userIds = new ConcurrentLinkedQueue<>();
        @EventListener public void record(UserWokeUpEvent event) { userIds.add(event.userId()); }
    }

    private User user() {
        UUID id = UUID.randomUUID();
        return users.saveAndFlush(new User(id, id.toString().substring(0, 30), "Test", "hash", "UTC"));
    }

    private void concurrentRecalculation(User user) throws Exception {
        when(features.buildFeatures(any())).thenReturn(new UserFeatures(Optional.of(1L), 1,
                ScreenState.ON, Optional.empty(), Optional.empty(), 0, ChargingState.UNKNOWN,
                Optional.empty(), ScheduleState.UNKNOWN, Optional.of(1L), Optional.empty()));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Void> task = () -> { start.await(); calculation.recalculate(user); return null; };
            var a = executor.submit(task);
            var b = executor.submit(task);
            start.countDown();
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);
        }
    }

    @Test
    void concurrentWakePublishesOnlyOnce() throws Exception {
        User user = user();
        states.saveAndFlush(new UserState(user, SleepState.SLEEPING, 0.8));
        concurrentRecalculation(user);
        assertThat(states.findById(user.getId()).orElseThrow().getSleepState()).isEqualTo(SleepState.AWAKE);
        assertThat(published.userIds.stream().filter(user.getId()::equals).count()).isEqualTo(1);
    }

    @Test
    void concurrentFirstCalculationCreatesOneStateWithoutWake() throws Exception {
        User user = user();
        concurrentRecalculation(user);
        assertThat(states.findById(user.getId())).isPresent();
        assertThat(published.userIds).doesNotContain(user.getId());
    }

    @Test
    void repositoryFiltersFutureAtReceiptAndCountsOnlyTheBoundedWindow() {
        User user = user();
        Instant now = Instant.now();
        DeviceEvent valid = new DeviceEvent(UUID.randomUUID(), user, DeviceEventType.PHONE_UNLOCKED, now.minusSeconds(60));
        DeviceEvent future = new DeviceEvent(UUID.randomUUID(), user, DeviceEventType.PHONE_UNLOCKED, now.plusSeconds(60));
        DeviceEvent invalidAtReceipt = new DeviceEvent(UUID.randomUUID(), user, DeviceEventType.PHONE_UNLOCKED, now.minusSeconds(30));
        org.springframework.test.util.ReflectionTestUtils.setField(invalidAtReceipt, "receivedAt", now.minusSeconds(40));
        events.saveAllAndFlush(List.of(valid, future, invalidAtReceipt));
        assertThat(events.findLatestForInference(user.getId(), now)).extracting(DeviceEvent::getEventId)
                .containsExactly(valid.getEventId());
        assertThat(events.countForInference(user, DeviceEventType.PHONE_UNLOCKED, now.minusSeconds(1800), now)).isEqualTo(1);
    }

    @Test
    void recentGoogleQueryDeduplicatesAndRejectsInvalidTimes() {
        User user = user();
        Instant now = Instant.now();
        var older = new SleepClassificationEvent(user, now.minusSeconds(660), 90, 1, 1);
        var same = new SleepClassificationEvent(user, now.minusSeconds(60), 95, 1, 1);
        var duplicate = new SleepClassificationEvent(user, now.minusSeconds(60), 10, 1, 1);
        var future = new SleepClassificationEvent(user, now.plusSeconds(60), 99, 1, 1);
        var invalid = new SleepClassificationEvent(user, now.minusSeconds(30), 99, 1, 1);
        org.springframework.test.util.ReflectionTestUtils.setField(invalid, "receivedAt", now.minusSeconds(40));
        var ancient = new SleepClassificationEvent(user, now.minusSeconds(3660), 99, 1, 1);
        classifications.saveAllAndFlush(List.of(older, same, duplicate, future, invalid, ancient));
        assertThat(classifications.findRecentForInference(user.getId(), now.minusSeconds(3600), now))
                .extracting(SleepClassificationEvent::getSleepConfidence)
                .containsExactly(10, 90);
    }

}
