package com.amiawake.amiawake.sleepschedule.service;

import com.amiawake.amiawake.common.exception.SleepScheduleNotFoundException;
import com.amiawake.amiawake.common.exception.UserNotFoundException;
import com.amiawake.amiawake.sleepschedule.dto.SleepScheduleEnabledRequest;
import com.amiawake.amiawake.sleepschedule.dto.SleepScheduleRequest;
import com.amiawake.amiawake.sleepschedule.dto.SleepScheduleResponse;
import com.amiawake.amiawake.sleepschedule.entity.SleepSchedule;
import com.amiawake.amiawake.sleepschedule.mapper.SleepScheduleMapper;
import com.amiawake.amiawake.sleepschedule.repository.SleepScheduleRepository;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.repository.UserRepository;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
public class SleepScheduleService {
    private final SleepScheduleRepository sleepScheduleRepository;
    private final UserRepository userRepository;
    private final RepositoryMethodInvocationListener repositoryMethodInvocationListener;

    public SleepScheduleService(
            SleepScheduleRepository sleepScheduleRepository, UserRepository userRepository,
            RepositoryMethodInvocationListener repositoryMethodInvocationListener
    ) {
        this.sleepScheduleRepository = sleepScheduleRepository;
        this.userRepository = userRepository;
        this.repositoryMethodInvocationListener = repositoryMethodInvocationListener;
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
    }

    private SleepSchedule getSleepScheduleByUser(User user) {
        return sleepScheduleRepository.findByUser(user).orElseThrow(SleepScheduleNotFoundException::new);
    }

    public SleepScheduleResponse getSleepSchedule(UUID userId) {
        User user = getUserById(userId);

        SleepSchedule schedule = getSleepScheduleByUser(user);

        return SleepScheduleMapper.toSleepScheduleResponse(schedule);
    }

    @Transactional
    public SleepScheduleResponse setSleepSchedule(UUID userId, SleepScheduleRequest request) {
        User user = getUserById(userId);

        Optional<SleepSchedule> optionalSchedule =
                sleepScheduleRepository.findByUser(user);

        SleepSchedule schedule;

        if (optionalSchedule.isPresent()) {
            schedule = optionalSchedule.get();

            schedule.changeSchedule(
                    request.sleepTime(),
                    request.wakeTime()
            );
        } else {
            schedule = new SleepSchedule(
                    user,
                    request.sleepTime(),
                    request.wakeTime()
            );

            sleepScheduleRepository.save(schedule);
        }

        return SleepScheduleMapper.toSleepScheduleResponse(schedule);
    }

    @Transactional
    public SleepScheduleResponse setEnabledStatus(UUID userId, SleepScheduleEnabledRequest request) {
        User user = getUserById(userId);

        SleepSchedule schedule = getSleepScheduleByUser(user);

        schedule.changeEnabled(request.enabled());

        return SleepScheduleMapper.toSleepScheduleResponse(schedule);
    }

    @Transactional
    public void deleteSleepSchedule(UUID userId) {
        User user = getUserById(userId);

        SleepSchedule schedule = getSleepScheduleByUser(user);

        sleepScheduleRepository.delete(schedule);
    }

    public boolean isUserSleeping(UUID userId) {
        User user = getUserById(userId);

        ZoneId zoneId = ZoneId.of(user.getTimeZone());
        LocalTime userTime = LocalTime.now(zoneId);

        SleepSchedule schedule = getSleepScheduleByUser(user);

        return schedule.isEnabled() && schedule.isSleepingAt(userTime);
    }
}
