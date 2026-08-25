package com.amiawake.amiawake.userstate.dto;

import com.amiawake.amiawake.inference.states.SleepState;

import java.time.Instant;
import java.util.Optional;

public record UserStateResponse(
        SleepState state,
        double confidence,
        Optional<Instant> calculatedAt
) {
}
