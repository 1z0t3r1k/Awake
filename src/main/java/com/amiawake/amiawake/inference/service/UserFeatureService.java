package com.amiawake.amiawake.inference.service;

import com.amiawake.amiawake.deviceevent.entity.DeviceEvent;
import com.amiawake.amiawake.deviceevent.entity.DeviceEventType;
import com.amiawake.amiawake.deviceevent.repository.DeviceEventRepository;
import com.amiawake.amiawake.inference.model.GoogleSleepFeature;
import com.amiawake.amiawake.inference.model.UserFeatures;
import com.amiawake.amiawake.inference.states.ChargingState;
import com.amiawake.amiawake.inference.states.ScheduleState;
import com.amiawake.amiawake.inference.states.ScreenState;
import com.amiawake.amiawake.sleepclassification.repository.SleepClassificationRepository;
import com.amiawake.amiawake.sleepschedule.repository.SleepScheduleRepository;
import com.amiawake.amiawake.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserFeatureService {
    // A heartbeat proves connectivity, not that an old screen/charging edge is still true.
    private static final Duration MAX_STATE_AGE = Duration.ofHours(12);
    private final DeviceEventRepository deviceEventRepository;
    private final SleepScheduleRepository sleepScheduleRepository;
    private final SleepClassificationRepository sleepClassificationRepository;
    private final Clock clock;

    @Autowired
    public UserFeatureService(
            DeviceEventRepository events, SleepScheduleRepository schedules,
            SleepClassificationRepository classifications
    ) {
        this(events, schedules, classifications, Clock.systemUTC());
    }

    UserFeatureService(
            DeviceEventRepository events, SleepScheduleRepository schedules,
            SleepClassificationRepository classifications, Clock clock
    ) {
        this.deviceEventRepository = events;
        this.sleepScheduleRepository = schedules;
        this.sleepClassificationRepository = classifications;
        this.clock = clock;
    }

    public UserFeatures buildFeatures(User user) {
        Instant now = clock.instant();
        Map<DeviceEventType, DeviceEvent> latest = new EnumMap<>(DeviceEventType.class);
        deviceEventRepository.findLatestForInference(user.getId(), now).stream()
                .filter(e -> validTime(e.getOccurredAt(), e.getReceivedAt(), now))
                .forEach(e -> latest.put(e.getType(), e));
        Optional<DeviceEvent> screen = latestState(
                latest, DeviceEventType.SCREEN_ON,
                DeviceEventType.SCREEN_OFF, now
        );
        Optional<DeviceEvent> charging = latestState(
                latest, DeviceEventType.CHARGING_STARTED,
                DeviceEventType.CHARGING_STOPPED, now
        );
        return new UserFeatures(
                age(Optional.ofNullable(latest.get(DeviceEventType.PHONE_UNLOCKED)), now),
                count(user, DeviceEventType.PHONE_UNLOCKED, now),
                screen.map(e -> e.getType() == DeviceEventType.SCREEN_ON ? ScreenState.ON : ScreenState.OFF)
                        .orElse(ScreenState.UNKNOWN),
                age(screen.filter(e -> e.getType() == DeviceEventType.SCREEN_OFF), now),
                age(Optional.ofNullable(latest.get(DeviceEventType.MOTION)), now),
                count(user, DeviceEventType.MOTION, now),
                charging.map(e -> e.getType() == DeviceEventType.CHARGING_STARTED
                        ? ChargingState.CHARGING : ChargingState.NOT_CHARGING).orElse(ChargingState.UNKNOWN),
                age(charging.filter(e -> e.getType() == DeviceEventType.CHARGING_STARTED), now),
                schedule(user, now),
                age(Optional.ofNullable(latest.get(DeviceEventType.HEARTBEAT)), now),
                google(user, now)
        );
    }

    private Optional<DeviceEvent> latestState(
            Map<DeviceEventType, DeviceEvent> events,
            DeviceEventType first, DeviceEventType second, Instant now
    ) {
        DeviceEvent a = events.get(first), b = events.get(second);

        if (a != null && b != null && a.getOccurredAt().equals(b.getOccurredAt())) {
            return Optional.empty();
        }
        DeviceEvent last = a == null ? b : b == null || a.getOccurredAt().isAfter(b.getOccurredAt()) ? a : b;
        return Optional.ofNullable(last)
                .filter(e -> !e.getOccurredAt().isBefore(now.minus(MAX_STATE_AGE)));
    }

    private Optional<Long> age(Optional<DeviceEvent> event, Instant now) {
        return event.filter(e -> validTime(e.getOccurredAt(), e.getReceivedAt(), now))
                .map(e -> Duration.between(e.getOccurredAt(), now).toMinutes());
    }

    private boolean validTime(Instant occurred, Instant received, Instant now) {
        // A timestamp that was in the future at ingestion must not become evidence later.
        return occurred != null && received != null && !occurred.isAfter(now) && !occurred.isAfter(received);
    }

    private long count(User user, DeviceEventType type, Instant now) {
        return deviceEventRepository.countForInference(user, type, now.minus(Duration.ofMinutes(30)), now);
    }

    private Optional<GoogleSleepFeature> google(User user, Instant now) {
        return sleepClassificationRepository.findLatestForInference(user.getId(), now)
                .filter(e -> validTime(e.getOccurredAt(), e.getReceivedAt(), now))
                .filter(e -> e.getSleepConfidence() >= 0 && e.getSleepConfidence() <= 100)
                .map(e -> new GoogleSleepFeature(
                        e.getSleepConfidence(),
                        Duration.between(e.getOccurredAt(), now).toMinutes()
                ));
    }

    private ScheduleState schedule(User user, Instant now) {
        return sleepScheduleRepository.findByUser(user).filter(s -> s.isEnabled()).map(s -> {
            try {
                if (user.getTimeZone() == null)
                    return ScheduleState.UNKNOWN;
                LocalTime local = now.atZone(ZoneId.of(user.getTimeZone())).toLocalTime();
                return s.isSleepingAt(local) ? ScheduleState.IN_SLEEP_WINDOW : ScheduleState.OUTSIDE_SLEEP_WINDOW;
            } catch (DateTimeException invalidZone) {
                return ScheduleState.UNKNOWN;
            }
        }).orElse(ScheduleState.UNKNOWN);
    }

    public Optional<Long> getMinutesSinceLastUnlock(User user) {
        return buildFeatures(user).minutesSinceLastUnlock();
    }

    public Optional<Long> getMinutesSinceLastMotion(User user) {
        return buildFeatures(user).minutesSinceLastMotion();
    }

    public Optional<Long> getMinutesSinceLastHeartbeat(User user) {
        return buildFeatures(user).minutesSinceLastHeartbeat();
    }

    public Optional<Long> getScreenOffDurationMinutes(User user) {
        return buildFeatures(user).screenOffDurationMinutes();
    }

    public Optional<Long> getChargingDurationMinutes(User user) {
        return buildFeatures(user).chargingDurationMinutes();
    }

    public ScreenState getScreenState(User user) {
        return buildFeatures(user).screenState();
    }

    public ChargingState getChargingState(User user) {
        return buildFeatures(user).chargingState();
    }

    public ScheduleState getScheduleState(User user) {
        return schedule(user, clock.instant());
    }

    public long unlocksLast30Minutes(User user) {
        return count(user, DeviceEventType.PHONE_UNLOCKED, clock.instant());
    }

    public long motionEventsLast30Minutes(User user) {
        return count(user, DeviceEventType.MOTION, clock.instant());
    }

    Optional<GoogleSleepFeature> getGoogleSleepFeature(User user) {
        return google(user, clock.instant());
    }
}
