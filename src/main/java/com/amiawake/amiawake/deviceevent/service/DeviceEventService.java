package com.amiawake.amiawake.deviceevent.service;

import com.amiawake.amiawake.common.exception.UserNotFoundException;
import com.amiawake.amiawake.deviceevent.dto.DeviceEventBatchRequest;
import com.amiawake.amiawake.deviceevent.dto.DeviceEventRequest;
import com.amiawake.amiawake.deviceevent.entity.DeviceEvent;
import com.amiawake.amiawake.deviceevent.entity.DeviceEventType;
import com.amiawake.amiawake.deviceevent.repository.DeviceEventBatchRepository;
import com.amiawake.amiawake.deviceevent.repository.DeviceEventRepository;
import com.amiawake.amiawake.user.entity.User;
import com.amiawake.amiawake.user.repository.UserRepository;
import com.amiawake.amiawake.userstate.service.UserStateCalculationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DeviceEventService {
    private final DeviceEventRepository deviceEventRepository;
    private final UserRepository userRepository;
    private final DeviceEventBatchRepository deviceEventBatchRepository;
    private final UserStateCalculationService userStateCalculationService;

    public DeviceEventService(
            DeviceEventRepository deviceEventRepository, UserRepository userRepository,
            DeviceEventBatchRepository deviceEventBatchRepository, UserStateCalculationService userStateCalculationService
    ) {
        this.deviceEventRepository = deviceEventRepository;
        this.userRepository = userRepository;
        this.deviceEventBatchRepository = deviceEventBatchRepository;
        this.userStateCalculationService = userStateCalculationService;
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional
    public boolean receiveEvent(UUID eventId, UUID userId, DeviceEventType type, Instant occurredAt) {
        User user = getUserById(userId);

        Instant receivedAt = Instant.now();

        int amountOfEditedRows = deviceEventRepository.insertIfAbsent(eventId, userId, type.name(), occurredAt, receivedAt);

        boolean inserted = amountOfEditedRows != 0;

        if (inserted) {
            recalculateStateIfNeeded(user, type);
        }

        return inserted;
    }

    @Transactional
    public void receiveBatch(UUID userId, DeviceEventBatchRequest request) {
        User user = getUserById(userId);
        List<DeviceEvent> deviceEventList = new ArrayList<>(request.events().size());

        for (DeviceEventRequest deviceEventRequest : request.events()) {
            deviceEventList.add(new DeviceEvent(
                    deviceEventRequest.eventId(),
                    user,
                    deviceEventRequest.type(),
                    deviceEventRequest.occurredAt()
            ));
        }

        deviceEventBatchRepository.insertBatch(deviceEventList);

        boolean containsUnlock = deviceEventList.stream()
                .anyMatch(event ->
                        event.getType() == DeviceEventType.PHONE_UNLOCKED
                );

        if (containsUnlock) {
            userStateCalculationService.recalculate(user);
        }
    }

    private void recalculateStateIfNeeded(User user, DeviceEventType type) {
        if (type == DeviceEventType.PHONE_UNLOCKED) {
            userStateCalculationService.recalculate(user);
        }
    }
}
