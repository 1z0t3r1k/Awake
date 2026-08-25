package com.amiawake.amiawake.inference.model;

import com.amiawake.amiawake.inference.states.SleepState;

public record InferenceResult(
        SleepState state,
        double confidence
) {
}
