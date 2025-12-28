package io.github.hyperf0rm.deep_vibe.entity;

import jakarta.persistence.*;

import io.github.hyperf0rm.deep_vibe.entity.User;
import io.github.hyperf0rm.deep_vibe.entity.Track;

import java.time.Instant;

@Entity
@Table(name = "scrobbles",
       indexes = { @Index(name = "idx_played_at", columnList = "played_at") })
public class Scrobble {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    private Instant playedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(Instant playedAt) {
        this.playedAt = playedAt;
    }
}
