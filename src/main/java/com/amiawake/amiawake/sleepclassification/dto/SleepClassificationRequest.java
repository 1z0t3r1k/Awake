package com.amiawake.amiawake.sleepclassification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record SleepClassificationRequest(
        @NotNull Instant occurredAt,
        @Min(0) @Max(100) int sleepConfidence,
        int motion,
        int light
) {
}