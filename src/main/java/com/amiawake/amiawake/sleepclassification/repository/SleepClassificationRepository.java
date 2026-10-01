package com.amiawake.amiawake.sleepclassification.repository;

import com.amiawake.amiawake.sleepclassification.entity.SleepClassificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface SleepClassificationRepository extends JpaRepository<SleepClassificationEvent, UUID> {
    @Query(value = """
            SELECT * FROM sleep_classification_events
            WHERE user_id = :userId AND occurred_at <= :now AND occurred_at <= received_at
            ORDER BY occurred_at DESC, sleep_confidence ASC, id DESC LIMIT 1
            """, nativeQuery = true)
    Optional<SleepClassificationEvent> findLatestForInference(@Param("userId") UUID userId,
                                                             @Param("now") Instant now);
}
