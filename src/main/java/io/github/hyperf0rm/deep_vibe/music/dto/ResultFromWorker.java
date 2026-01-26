package io.github.hyperf0rm.deep_vibe.music.dto;
import java.util.List;

public record ResultFromWorker(
        Long id,
        Short bpm,
        Float rms,
        Float centroid,
        float[] embedding) {
}
