package com.amiawake.amiawake.sleepclassification.service;

import com.amiawake.amiawake.common.exception.UserNotFoundException;
import com.amiawake.amiawake.sleepclassification.dto.SleepClassificationRequest;
import com.amiawake.amiawake.sleepclassification.entity.SleepClassificationEvent;
import com.amiawake.amiawake.sleepclassification.repository.SleepClassificationRepository;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.repository.UserRepository;
import com.amiawake.amiawake.user.service.UserService;
import com.amiawake.amiawake.userstate.service.UserStateCalculationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SleepClassificationService {
    private final SleepClassificationRepository sleepClassificationRepository;
    private final UserRepository userRepository;
    private final UserStateCalculationService userStateCalculationService;

    public SleepClassificationService(
            SleepClassificationRepository sleepClassificationRepository,
            UserRepository userRepository, UserStateCalculationService userStateCalculationService
    ) {
        this.sleepClassificationRepository = sleepClassificationRepository;
        this.userRepository = userRepository;
        this.userStateCalculationService = userStateCalculationService;
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
    }

    @Transactional
    public void receiveClassification(UUID userId, SleepClassificationRequest request) {
        User user = getUserById(userId);

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
