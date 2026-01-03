package io.github.hyperf0rm.deep_vibe.repository;

import io.github.hyperf0rm.deep_vibe.entity.Track;
import io.github.hyperf0rm.deep_vibe.enums.TrackQueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackRepository extends JpaRepository<Track, Long> {

    public Track findByNameAndArtistName(String name, String artistName);

    public List<Track> findByPreviewUrlIsNull();

    public List<Track> findByPreviewUrlIsNotNullAndStatus(TrackQueueStatus status);
}
