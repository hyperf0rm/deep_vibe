package io.github.hyperf0rm.deep_vibe.exception;

import java.time.Instant;

public record APIErrorResponse(
        int status,
        String error,
        Instant timestamp
) {
}
