package com.amiawake.amiawake.inference.model;

public record GoogleSleepFeature(
        int googleSleepConfidence,
        long minutesSinceLastGoogleSleepClassification,
        int sampleCount,
        int lowConfidenceCount
) {
    public GoogleSleepFeature(int confidence, long age) {
        this(confidence, age, 1, confidence <= 20 ? 1 : 0);
    }
}
