package io.github.hyperf0rm.deep_vibe.music.repository;

import io.github.hyperf0rm.deep_vibe.music.entity.Track;
import io.github.hyperf0rm.deep_vibe.user.User;
import io.github.hyperf0rm.deep_vibe.music.entity.TrackQueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TrackRepository extends JpaRepository<Track, Long> {

    public Track findByNameAndArtistName(String name, String artistName);

    public List<Track> findByPreviewUrlIsNull();

    public List<Track> findByPreviewUrlIsNotNullAndStatus(TrackQueueStatus status);

    @Query("SELECT s.track FROM Scrobble s WHERE s.user = :user AND s.track.status = :status ORDER BY s.playedAt DESC")
    public List<Track> findByUserScrobblesAndStatus(@Param("user") User user, @Param("status") TrackQueueStatus status);

    @Query("SELECT s.track " +
            "FROM Scrobble s " +
            "WHERE s.user = :user " +
            "AND s.track.status = :status " +
            "AND s.playedAt >= :from " +
            "AND s.playedAt <= :to " +
            "ORDER BY s.playedAt DESC")
    public List<Track> findByUserScrobblesAndStatusFilterByPlayedAt(
            @Param("user") User user, @Param("status") TrackQueueStatus status, Instant from, Instant to
    );

}
