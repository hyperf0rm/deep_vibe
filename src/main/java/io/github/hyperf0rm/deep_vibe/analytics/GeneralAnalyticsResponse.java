package io.github.hyperf0rm.deep_vibe.analytics;

public record GeneralAnalyticsResponse(
        String username,
        Integer bpm,
        Float rms,
        Float spectralCentroid
) { }
