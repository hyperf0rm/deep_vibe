package io.github.hyperf0rm.deep_vibe.repository;

import io.github.hyperf0rm.deep_vibe.entity.Scrobble;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrobbleRepository extends JpaRepository<Scrobble, Long> { }
