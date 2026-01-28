package io.github.hyperf0rm.deep_vibe.music.repository;

import io.github.hyperf0rm.deep_vibe.music.entity.Track;
import io.github.hyperf0rm.deep_vibe.user.User;
import io.github.hyperf0rm.deep_vibe.music.entity.TrackQueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TrackRepository extends JpaRepository<Track, Long> {

    public Track findByNameAndArtistName(String name, String artistName);

    public List<Track> findByPreviewUrlIsNullAndStatusNot(TrackQueueStatus status);

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

    @Query(value = "SELECT * " +
            "FROM tracks " +
            "WHERE id != :trackId " +
            "ORDER BY embedding <=> " +
            "(SELECT embedding FROM tracks WHERE id = :trackId)" +
            "LIMIT :limit",
           nativeQuery = true)
    public List<TrackProjection> findSimilarTracks(Long trackId, int limit);

    Optional<TrackProjection> findTrackProjectionById(Long id);

}
