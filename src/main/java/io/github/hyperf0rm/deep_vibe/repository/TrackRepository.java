package io.github.hyperf0rm.deep_vibe.repository;

import io.github.hyperf0rm.deep_vibe.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, Integer> {

    public Track findByNameAndArtistName(String name, String artistName);
}
