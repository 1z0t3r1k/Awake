package com.amiawake.amiawake.userstate.service;

import com.amiawake.amiawake.inference.model.*;
import com.amiawake.amiawake.inference.service.*;
import com.amiawake.amiawake.inference.states.SleepState;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.repository.UserRepository;
import com.amiawake.amiawake.userstate.entity.UserState;
import com.amiawake.amiawake.userstate.event.UserWokeUpEvent;
import com.amiawake.amiawake.userstate.repository.UserStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.util.*;
import static org.mockito.Mockito.*;

class UserStateCalculationServiceTest {
    private final User user = new User(UUID.randomUUID(), "test", "Test", "hash", "UTC");
    private final UserFeatureService features = mock(UserFeatureService.class);
    private final InferenceService inference = mock(InferenceService.class);
    private final UserStateService states = mock(UserStateService.class);
    private final UserStateRepository repository = mock(UserStateRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final UserStateCalculationService service = new UserStateCalculationService(features, inference,
            states, repository, users, publisher);

    @Test
    void locksBeforeReadingAndPublishesOnlyOnceAfterSavingSleepingToAwake() {
        var snapshot = mock(UserFeatures.class);
        var result = new InferenceResult(SleepState.AWAKE, 0.98);
        var state = new UserState(user, SleepState.SLEEPING, 0.8);
        when(features.buildFeatures(user)).thenReturn(snapshot);
        when(inference.infer(snapshot)).thenReturn(result);
        when(repository.findById(user.getId())).thenReturn(Optional.of(state));
        doAnswer(invocation -> { state.updateState(result.state(), result.confidence()); return null; })
                .when(states).upsertUserState(user, result);
        service.recalculate(user);
        service.recalculate(user);
        var order = inOrder(users, features, repository, states, publisher);
        order.verify(users).lockForInference(user.getId());
        order.verify(features).buildFeatures(user);
        order.verify(repository).findById(user.getId());
        order.verify(states).upsertUserState(user, result);
        order.verify(publisher).publishEvent(new UserWokeUpEvent(user.getId()));
        verify(publisher, times(1)).publishEvent(any(UserWokeUpEvent.class));
    }

    @Test
    void otherTransitionsAndFirstStateDoNotPublish() {
        var snapshot = mock(UserFeatures.class);
        when(features.buildFeatures(user)).thenReturn(snapshot);
        for (SleepState old : SleepState.values()) {
            for (SleepState next : SleepState.values()) {
                if (old == SleepState.SLEEPING && next == SleepState.AWAKE) continue;
                when(repository.findById(user.getId())).thenReturn(Optional.of(new UserState(user, old, 0)));
                when(inference.infer(snapshot)).thenReturn(new InferenceResult(next, 0));
                service.recalculate(user);
            }
        }
        when(repository.findById(user.getId())).thenReturn(Optional.empty());
        when(inference.infer(snapshot)).thenReturn(new InferenceResult(SleepState.AWAKE, 0.98));
        service.recalculate(user);
        verifyNoInteractions(publisher);
    }

    @Test
    void staleSleepingStateDoesNotAnnounceWakeAfterAnOutage() {
        var snapshot = mock(UserFeatures.class);
        var state = new UserState(user, SleepState.SLEEPING, 0.8);
        org.springframework.test.util.ReflectionTestUtils.setField(state, "calculatedAt",
                java.time.Instant.now().minusSeconds(91 * 60));
        when(features.buildFeatures(user)).thenReturn(snapshot);
        when(inference.infer(snapshot)).thenReturn(new InferenceResult(SleepState.AWAKE, 0.98));
        when(repository.findById(user.getId())).thenReturn(Optional.of(state));
        service.recalculate(user);
        verifyNoInteractions(publisher);
    }

    @Test
    void failedSaveDoesNotPublish() {
        var snapshot = mock(UserFeatures.class);
        var result = new InferenceResult(SleepState.AWAKE, 0.98);
        when(features.buildFeatures(user)).thenReturn(snapshot);
        when(inference.infer(snapshot)).thenReturn(result);
        when(repository.findById(user.getId())).thenReturn(Optional.of(new UserState(user, SleepState.SLEEPING, 0.8)));
        doThrow(new IllegalStateException("save failed")).when(states).upsertUserState(user, result);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.recalculate(user))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(publisher);
    }

    @Test
    void shortAmbiguityKeepsSleepWithoutMovingItsOriginalTimestamp() {
        var snapshot = mock(UserFeatures.class);
        var state = new UserState(user, SleepState.SLEEPING, .85);
        var original = java.time.Instant.now().minusSeconds(30 * 60);
        org.springframework.test.util.ReflectionTestUtils.setField(state, "calculatedAt", original);
        when(features.buildFeatures(user)).thenReturn(snapshot);
        when(inference.infer(snapshot)).thenReturn(new InferenceResult(SleepState.UNKNOWN, 0));
        when(inference.canRetainSleep(snapshot)).thenReturn(true);
        when(repository.findById(user.getId())).thenReturn(Optional.of(state));
        for (int i = 0; i < 10; i++) {
            var result = service.recalculate(user);
            org.assertj.core.api.Assertions.assertThat(result.state()).isEqualTo(SleepState.SLEEPING);
            org.assertj.core.api.Assertions.assertThat(result.confidence()).isEqualTo(.60);
        }
        org.assertj.core.api.Assertions.assertThat(state.getCalculatedAt()).isEqualTo(original);
        verifyNoInteractions(states, publisher);

        org.springframework.test.util.ReflectionTestUtils.setField(state, "calculatedAt", java.time.Instant.now().minusSeconds(91 * 60));
        service.recalculate(user);
        verify(states).upsertUserState(user, new InferenceResult(SleepState.UNKNOWN, 0));
    }

    @Test
    void wakeAfterShortGapPublishesOnceAndAwakeDoesNotGetTheLongSleepTtl() {
        var snapshot = mock(UserFeatures.class);
        var state = new UserState(user, SleepState.SLEEPING, .60);
        var past = java.time.Instant.now().minusSeconds(30 * 60);
        org.springframework.test.util.ReflectionTestUtils.setField(state, "calculatedAt", past);
        when(features.buildFeatures(user)).thenReturn(snapshot);
        var result = new InferenceResult(SleepState.AWAKE, .98);
        when(inference.infer(snapshot)).thenReturn(result);
        when(repository.findById(user.getId())).thenReturn(Optional.of(state));
        doAnswer(invocation -> { state.updateState(result.state(), result.confidence()); return null; })
                .when(states).upsertUserState(user, result);
        service.recalculate(user);
        service.recalculate(user);
        verify(publisher, times(1)).publishEvent(new UserWokeUpEvent(user.getId()));
        org.springframework.test.util.ReflectionTestUtils.setField(state, "calculatedAt", past);
        org.assertj.core.api.Assertions.assertThat(state.isFreshAt(java.time.Instant.now())).isFalse();
    }

    @Test
    void contradictoryActivityClearsSleepEvenDuringTheHoldWindow() {
        var snapshot = mock(UserFeatures.class);
        when(features.buildFeatures(user)).thenReturn(snapshot);
        var result = new InferenceResult(SleepState.UNKNOWN, 0);
        when(inference.infer(snapshot)).thenReturn(result);
        when(inference.canRetainSleep(snapshot)).thenReturn(false);
        when(repository.findById(user.getId())).thenReturn(Optional.of(new UserState(user, SleepState.SLEEPING, .8)));
        service.recalculate(user);
        verify(states).upsertUserState(user, result);
        verifyNoInteractions(publisher);
    }

}
