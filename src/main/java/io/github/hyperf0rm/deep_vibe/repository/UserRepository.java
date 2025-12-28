package io.github.hyperf0rm.deep_vibe.repository;

import io.github.hyperf0rm.deep_vibe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> { }
