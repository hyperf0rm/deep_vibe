package io.github.hyperf0rm.deep_vibe.user;

import java.time.Instant;

public record UserResponse(
        Integer id,
        String lastfmUsername,
        Instant createdAt
) { }
