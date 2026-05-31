package com.adhamamr.passwordy.exception;

/**
 * Thrown when an authenticated user tries to access a resource they do not own. Mapped to
 * HTTP 403 by the global handler.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
