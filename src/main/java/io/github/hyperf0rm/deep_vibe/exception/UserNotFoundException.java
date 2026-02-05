package io.github.hyperf0rm.deep_vibe.exception;

public class UserNotFoundException extends RuntimeException {

    private final String username;

    public UserNotFoundException(String message, String username) {
        super(message);
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }
}
