package com.amiawake.amiawake.userstate.scheduler;

import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.repository.UserRepository;
import com.amiawake.amiawake.userstate.service.UserStateCalculationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserStateScheduler {
    private final UserRepository userRepository;
    private final UserStateCalculationService userStateCalculationService;

    public UserStateScheduler(UserRepository userRepository, UserStateCalculationService userStateCalculationService) {
        this.userRepository = userRepository;
        this.userStateCalculationService = userStateCalculationService;
    }

    // TODO: Когда появятся проблемы с findAll -> внедрение Kafka/Batching
    @Scheduled(cron = "0 */15 * * * *")
    public void recalculateUserStates() {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            userStateCalculationService.recalculate(user);
        }
    }
}
