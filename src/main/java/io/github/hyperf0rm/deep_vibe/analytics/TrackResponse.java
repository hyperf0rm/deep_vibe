package io.github.hyperf0rm.deep_vibe.analytics;

public record TrackResponse(
        Long id,
        String artistName,
        String name,
        short bpm,
        float rms,
        float spectralCentroid,
        String previewUrl
) {
}
