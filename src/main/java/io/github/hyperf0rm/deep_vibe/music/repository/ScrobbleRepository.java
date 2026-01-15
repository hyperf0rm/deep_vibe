package io.github.hyperf0rm.deep_vibe.music.repository;

import io.github.hyperf0rm.deep_vibe.music.entity.Scrobble;
import io.github.hyperf0rm.deep_vibe.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScrobbleRepository extends JpaRepository<Scrobble, Long> {

    public List<Scrobble> findScrobblesByUser(User user);
}
