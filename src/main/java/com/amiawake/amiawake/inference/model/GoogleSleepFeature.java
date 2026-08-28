package com.amiawake.amiawake.inference.model;

public record GoogleSleepFeature(
        int googleSleepConfidence,
        long minutesSinceLastGoogleSleepClassification
) {
}
