package com.amiawake.amiawake.sleepclassification.service;

import com.amiawake.amiawake.sleepclassification.dto.SleepClassificationRequest;
import com.amiawake.amiawake.sleepclassification.entity.SleepClassificationEvent;
import com.amiawake.amiawake.sleepclassification.repository.SleepClassificationRepository;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.service.UserService;
import com.amiawake.amiawake.userstate.service.UserStateCalculationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SleepClassificationService {
    private final SleepClassificationRepository sleepClassificationRepository;
    private final UserService userService;
    private final UserStateCalculationService userStateCalculationService;

    public SleepClassificationService(
            SleepClassificationRepository sleepClassificationRepository,
            UserService userService, UserStateCalculationService userStateCalculationService
    ) {
        this.sleepClassificationRepository = sleepClassificationRepository;
        this.userService = userService;
        this.userStateCalculationService = userStateCalculationService;
    }

    @Transactional
    public void receiveClassification(UUID userId, SleepClassificationRequest request) {
        User user = userService.getUserById(userId);

        SleepClassificationEvent event =
                new SleepClassificationEvent(
                        user,
                        request.occurredAt(),
                        request.sleepConfidence(),
                        request.motion(),
                        request.light()
                );

        sleepClassificationRepository.save(event);
        userStateCalculationService.recalculate(user);
    }
}
