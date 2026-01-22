package io.github.hyperf0rm.deep_vibe.music.repository;

import io.github.hyperf0rm.deep_vibe.music.entity.Scrobble;
import io.github.hyperf0rm.deep_vibe.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ScrobbleRepository extends JpaRepository<Scrobble, Long> {

    @Query("SELECT s " +
            "FROM Scrobble s " +
            "JOIN FETCH s.track " +
            "WHERE s.user = :user " +
            "ORDER BY s.playedAt ASC")
    public List<Scrobble> findByUser(@Param("user") User user);

    @Query("SELECT s " +
            "FROM Scrobble s " +
            "JOIN FETCH s.track " +
            "WHERE s.user = :user " +
            "AND s.playedAt >= :from " +
            "AND s.playedAt <= :to " +
            "ORDER BY s.playedAt ASC")
    public List<Scrobble> findByUserFilterByPlayedAt(
            @Param("user") User user,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
