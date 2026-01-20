package io.github.hyperf0rm.deep_vibe.exception;

public class ExternalAPIException extends RuntimeException {

    private final int statusCode;

    public ExternalAPIException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
