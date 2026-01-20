package io.github.hyperf0rm.deep_vibe.analytics;

import java.time.LocalDate;

public record TimelineResponse(
        String username,
        LocalDate date,
        Integer bpm,
        Float rms,
        Float spectralCentroid
) {
}
