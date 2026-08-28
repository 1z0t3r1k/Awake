package com.amiawake.amiawake.sleepclassification.repository;

import com.amiawake.amiawake.sleepclassification.entity.SleepClassificationEvent;
import com.amiawake.amiawake.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SleepClassificationRepository extends JpaRepository<SleepClassificationEvent, UUID> {
    Optional<SleepClassificationEvent> findTopByUserOrderByOccurredAtDesc(User user);
}
