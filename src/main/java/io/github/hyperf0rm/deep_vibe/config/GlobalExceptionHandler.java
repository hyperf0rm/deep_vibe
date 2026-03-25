package io.github.hyperf0rm.deep_vibe.config;

import io.github.hyperf0rm.deep_vibe.exception.APIErrorResponse;
import io.github.hyperf0rm.deep_vibe.exception.ExternalAPIException;
import io.github.hyperf0rm.deep_vibe.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ExternalAPIException.class)
    public ResponseEntity<APIErrorResponse> handleExternalAPIException(ExternalAPIException e) {
        log.error("External API error occurred: status={}, message={}", e.getStatusCode(), e.getMessage());
        APIErrorResponse response = new APIErrorResponse(
                e.getStatusCode(),
                e.getMessage(),
                Instant.now()
        );
        return ResponseEntity
                .status(e.getStatusCode() == 429 ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_GATEWAY)
                .body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<APIErrorResponse> handleUserNotFoundException(UserNotFoundException e) {
        log.info("User '{}' not found on Last.fm",  e.getUsername());
        APIErrorResponse response = new APIErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                e.getMessage(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIErrorResponse> handleGenericException(Exception e) {
        log.error("An unexpected error occurred: {}", e.getMessage());
        APIErrorResponse response = new APIErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

}
