package io.github.hyperf0rm.deep_vibe.music.repository;

import io.github.hyperf0rm.deep_vibe.music.entity.TrackQueueStatus;

public interface TrackProjection {
    Long getId();
    String getName();
    String getArtistName();
    short getBpm();
    float getRms();
    float getSpectralCentroid();
    String getPreviewUrl();
    TrackQueueStatus getStatus();
}
