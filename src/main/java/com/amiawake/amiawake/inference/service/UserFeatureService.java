package com.amiawake.amiawake.inference.service;

import com.amiawake.amiawake.deviceevent.entity.DeviceEvent;
import com.amiawake.amiawake.deviceevent.entity.DeviceEventType;
import com.amiawake.amiawake.deviceevent.repository.DeviceEventRepository;
import com.amiawake.amiawake.inference.model.UserFeatures;
import com.amiawake.amiawake.inference.states.ChargingState;
import com.amiawake.amiawake.inference.states.ScheduleState;
import com.amiawake.amiawake.inference.states.ScreenState;
import com.amiawake.amiawake.sleepschedule.entity.SleepSchedule;
import com.amiawake.amiawake.sleepschedule.repository.SleepScheduleRepository;
import com.amiawake.amiawake.user.entity.User;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class UserFeatureService {
    private final DeviceEventRepository deviceEventRepository;
    private final SleepScheduleRepository sleepScheduleRepository;

    public UserFeatureService(DeviceEventRepository deviceEventRepository, SleepScheduleRepository sleepScheduleRepository) {
        this.deviceEventRepository = deviceEventRepository;
        this.sleepScheduleRepository = sleepScheduleRepository;
    }

    // Get minutes since last ...
    public Optional<Long> getMinutesSinceLastUnlock(User user) {
        Optional<DeviceEvent> optionalDeviceEvent = deviceEventRepository.findFirstByUserAndTypeOrderByOccurredAtDesc(
                user,
                DeviceEventType.PHONE_UNLOCKED
        );

        if (optionalDeviceEvent.isPresent()) {
            DeviceEvent deviceEvent = optionalDeviceEvent.get();
            Instant currentTime = Instant.now();
            Instant eventTime = deviceEvent.getOccurredAt();
            if (currentTime.isBefore(eventTime)) {
                return Optional.empty();
            }
            return Optional.of(Duration.between(eventTime, currentTime).toMinutes());
        } else {
            return Optional.empty();
        }
    }

    public Optional<Long> getMinutesSinceLastMotion(User user) {
        Optional<DeviceEvent> optionalDeviceEvent = deviceEventRepository.findFirstByUserAndTypeOrderByOccurredAtDesc(
                user,
                DeviceEventType.MOTION
        );

        if (optionalDeviceEvent.isPresent()) {
            DeviceEvent deviceEvent = optionalDeviceEvent.get();
            Instant currentTime = Instant.now();
            Instant eventTime = deviceEvent.getOccurredAt();
            if (currentTime.isBefore(eventTime)) {
                return Optional.empty();
            }
            return Optional.of(Duration.between(eventTime, currentTime).toMinutes());
        } else {
            return Optional.empty();
        }
    }

    public Optional<Long> getMinutesSinceLastHeartbeat(User user) {
        Optional<DeviceEvent> optionalDeviceEvent = deviceEventRepository.findFirstByUserAndTypeOrderByOccurredAtDesc(
                user,
                DeviceEventType.HEARTBEAT
        );

        if (optionalDeviceEvent.isPresent()) {
            DeviceEvent deviceEvent = optionalDeviceEvent.get();
            Instant currentTime = Instant.now();
            Instant eventTime = deviceEvent.getOccurredAt();

            if (currentTime.isBefore(eventTime)) {
                return Optional.empty();
            }
            return Optional.of(Duration.between(eventTime, currentTime).toMinutes());
        } else {
            return Optional.empty();
        }
    }

    // ... last 30 minutes
    public long unlocksLast30Minutes(User user) {
        Instant currentTime = Instant.now();
        return deviceEventRepository.countDeviceEventsByUserAndTypeAndOccurredAtAfter(
                user,
                DeviceEventType.PHONE_UNLOCKED,
                currentTime.minus(Duration.ofMinutes(30))
        );
    }

    public long motionEventsLast30Minutes(User user) {
        Instant currentTime = Instant.now();
        return deviceEventRepository.countDeviceEventsByUserAndTypeAndOccurredAtAfter(
                user,
                DeviceEventType.MOTION,
                currentTime.minus(Duration.ofMinutes(30))
        );
    }

    // get ... duration minutes
    public Optional<Long> getScreenOffDurationMinutes(User user) {
        Optional<DeviceEvent> lastScreenEvent = deviceEventRepository.findFirstByUserAndTypeInOrderByOccurredAtDesc(
                user,
                List.of(
                        DeviceEventType.SCREEN_ON,
                        DeviceEventType.SCREEN_OFF
                )
        );

        if (lastScreenEvent.isPresent()) {
            DeviceEvent deviceEvent = lastScreenEvent.get();

            if (deviceEvent.getType() == DeviceEventType.SCREEN_ON) {
                return Optional.empty();
            }

            Instant currentTime = Instant.now();
            Instant eventTime = deviceEvent.getOccurredAt();

            if (currentTime.isBefore(eventTime)) {
                return Optional.empty();
            }

            return Optional.of(Duration.between(eventTime, currentTime).toMinutes());
        } else {
            return Optional.empty();
        }
    }

    public Optional<Long> getChargingDurationMinutes(User user) {
        Optional<DeviceEvent> optionalDeviceEvent = deviceEventRepository.findFirstByUserAndTypeInOrderByOccurredAtDesc(
                user,
                List.of(
                        DeviceEventType.CHARGING_STARTED,
                        DeviceEventType.CHARGING_STOPPED
                )
        );

        if (optionalDeviceEvent.isPresent()) {
            DeviceEvent deviceEvent = optionalDeviceEvent.get();

            if (deviceEvent.getType() == DeviceEventType.CHARGING_STOPPED) {
                return Optional.empty();
            }

            Instant currentTime = Instant.now();
            Instant eventTime = deviceEvent.getOccurredAt();

            if (currentTime.isBefore(eventTime)) {
                return Optional.empty();
            }

            return Optional.of(Duration.between(eventTime, currentTime).toMinutes());
        } else {
            return Optional.empty();
        }
    }

    // get ... state
    public ChargingState getChargingState(User user) {
        Optional<DeviceEvent> optionalDeviceEvent =
                deviceEventRepository.findFirstByUserAndTypeInOrderByOccurredAtDesc(
                        user,
                        List.of(
                                DeviceEventType.CHARGING_STARTED,
                                DeviceEventType.CHARGING_STOPPED
                        )
                );

        if (optionalDeviceEvent.isEmpty()) {
            return ChargingState.UNKNOWN;
        }

        DeviceEvent deviceEvent = optionalDeviceEvent.get();

        if (deviceEvent.getType() == DeviceEventType.CHARGING_STARTED) {
            return ChargingState.CHARGING;
        }

        return ChargingState.NOT_CHARGING;
    }

    public ScheduleState getScheduleState(User user) {
        Optional<SleepSchedule> optionalSchedule = sleepScheduleRepository.findByUser(user);

        if (optionalSchedule.isEmpty()) {
            return ScheduleState.UNKNOWN;
        }

        SleepSchedule schedule = optionalSchedule.get();

        if (!schedule.isEnabled()) {
            return ScheduleState.UNKNOWN;
        }

        ZoneId userZone = ZoneId.of(user.getTimeZone());
        LocalTime currentUserTime = LocalTime.now(userZone);

        if (schedule.isSleepingAt(currentUserTime)) {
            return ScheduleState.IN_SLEEP_WINDOW;
        }

        return ScheduleState.OUTSIDE_SLEEP_WINDOW;
    }

    public ScreenState getScreenState(User user) {
        Optional<DeviceEvent> optionalDeviceEvent =
                deviceEventRepository.findFirstByUserAndTypeInOrderByOccurredAtDesc(
                        user,
                        List.of(
                                DeviceEventType.SCREEN_ON,
                                DeviceEventType.SCREEN_OFF
                        )
                );

        if (optionalDeviceEvent.isEmpty()) {
            return ScreenState.UNKNOWN;
        }

        DeviceEvent deviceEvent = optionalDeviceEvent.get();

        if (deviceEvent.getType() == DeviceEventType.SCREEN_ON) {
            return ScreenState.ON;
        }

        return ScreenState.OFF;
    }

    public UserFeatures buildFeatures(User user) {
        return new UserFeatures(
                getMinutesSinceLastUnlock(user),
                unlocksLast30Minutes(user),
                getScreenState(user),
                getMinutesSinceLastMotion(user),
                getScreenOffDurationMinutes(user),
                motionEventsLast30Minutes(user),
                getChargingState(user),
                getChargingDurationMinutes(user),
                getScheduleState(user),
                getMinutesSinceLastHeartbeat(user)
        );
    }
}
