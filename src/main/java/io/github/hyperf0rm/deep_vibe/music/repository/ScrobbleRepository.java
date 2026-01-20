package io.github.hyperf0rm.deep_vibe.music.repository;

import io.github.hyperf0rm.deep_vibe.music.entity.Scrobble;
import io.github.hyperf0rm.deep_vibe.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface ScrobbleRepository extends JpaRepository<Scrobble, Long> {

    public List<Scrobble> findScrobblesByUser(User user);

    @Query("SELECT s " +
            "FROM Scrobble s " +
            "WHERE s.user = :user " +
            "AND s.playedAt >= :from " +
            "AND s.playedAt <= :to " +
            "ORDER BY s.playedAt DESC")
    public List<Scrobble> findScrobblesByUserFilterByPlayedAt(User user, Instant from, Instant to);
}
