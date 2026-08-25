package com.amiawake.amiawake.sleepschedule.repository;

import com.amiawake.amiawake.sleepschedule.entity.SleepSchedule;
import com.amiawake.amiawake.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SleepScheduleRepository extends JpaRepository<SleepSchedule, UUID> {
    Optional<SleepSchedule> findByUser(User user);
}
