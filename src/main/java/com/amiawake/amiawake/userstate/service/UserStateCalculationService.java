package com.amiawake.amiawake.userstate.service;

import com.amiawake.amiawake.inference.model.InferenceResult;
import com.amiawake.amiawake.inference.model.UserFeatures;
import com.amiawake.amiawake.inference.service.InferenceService;
import com.amiawake.amiawake.inference.service.UserFeatureService;
import com.amiawake.amiawake.inference.states.SleepState;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.repository.UserRepository;
import com.amiawake.amiawake.userstate.entity.UserState;
import com.amiawake.amiawake.userstate.event.UserWokeUpEvent;
import com.amiawake.amiawake.userstate.repository.UserStateRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserStateCalculationService {

    private final UserFeatureService userFeatureService;
    private final InferenceService inferenceService;
    private final UserStateService userStateService;
    private final UserStateRepository userStateRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public UserStateCalculationService(
            UserFeatureService userFeatureService,
            InferenceService inferenceService,
            UserStateService userStateService, UserStateRepository userStateRepository,
            UserRepository userRepository,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.userFeatureService = userFeatureService;
        this.inferenceService = inferenceService;
        this.userStateService = userStateService;
        this.userStateRepository = userStateRepository;
        this.userRepository = userRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public InferenceResult recalculate(User user) {
        userRepository.lockForInference(user.getId());
        UserFeatures features = userFeatureService.buildFeatures(user);

        InferenceResult result = inferenceService.infer(features);
        SleepState newState = result.state();
        Optional<UserState> optionalOldState = userStateRepository.findById(user.getId());

        SleepState oldState = optionalOldState.filter(state -> state.isFreshAt(java.time.Instant.now()))
                .map(UserState::getSleepState)
                .orElse(SleepState.UNKNOWN);
        userStateService.upsertUserState(user, result);
        if (oldState == SleepState.SLEEPING && newState == SleepState.AWAKE) {
            applicationEventPublisher.publishEvent(new UserWokeUpEvent(user.getId()));
        }

        return result;
    }
}