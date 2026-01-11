package io.github.hyperf0rm.deep_vibe.repository;

import io.github.hyperf0rm.deep_vibe.entity.Scrobble;
import io.github.hyperf0rm.deep_vibe.entity.Track;
import io.github.hyperf0rm.deep_vibe.entity.User;
import io.github.hyperf0rm.deep_vibe.enums.TrackQueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrackRepository extends JpaRepository<Track, Long> {

    public Track findByNameAndArtistName(String name, String artistName);

    public List<Track> findByPreviewUrlIsNull();

    public List<Track> findByPreviewUrlIsNotNullAndStatus(TrackQueueStatus status);

    @Query("SELECT s.track FROM Scrobble s WHERE s.user = :user AND s.track.status = :status ORDER BY s.playedAt DESC")
    public List<Track> findByUserScrobblesAndStatus(@Param("user") User user, @Param("status") TrackQueueStatus status);

}
