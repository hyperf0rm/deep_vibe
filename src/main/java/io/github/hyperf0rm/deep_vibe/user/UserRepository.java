package io.github.hyperf0rm.deep_vibe.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

    public User findByLastfmUsername(String username);
}
