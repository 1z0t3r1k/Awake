package com.amiawake.amiawake.friendship.dto;

import com.amiawake.amiawake.inference.states.SleepState;
import com.amiawake.amiawake.user.entity.User.AvailabilityStatus;

import java.time.Instant;
import java.util.Optional;

public record FriendResponse(
        String username,
        String displayName,
        AvailabilityStatus status,
        SleepState sleepState,
        double sleepConfidence,
        Optional<Instant> sleepStateCalculatedAt
) {
}
