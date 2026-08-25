package com.amiawake.amiawake.userstate.service;

import com.amiawake.amiawake.inference.model.InferenceResult;
import com.amiawake.amiawake.inference.model.UserFeatures;
import com.amiawake.amiawake.inference.service.InferenceService;
import com.amiawake.amiawake.inference.service.UserFeatureService;
import com.amiawake.amiawake.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserStateCalculationService {

    private final UserFeatureService userFeatureService;
    private final InferenceService inferenceService;
    private final UserStateService userStateService;

    public UserStateCalculationService(
            UserFeatureService userFeatureService,
            InferenceService inferenceService,
            UserStateService userStateService
    ) {
        this.userFeatureService = userFeatureService;
        this.inferenceService = inferenceService;
        this.userStateService = userStateService;
    }

    @Transactional
    public InferenceResult recalculate(User user) {
        UserFeatures features = userFeatureService.buildFeatures(user);

        InferenceResult result = inferenceService.infer(features);

        userStateService.upsertUserState(user, result);

        return result;
    }
}