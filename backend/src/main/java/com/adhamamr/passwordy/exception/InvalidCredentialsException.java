package com.adhamamr.passwordy.exception;

/**
 * Thrown on a failed login — whether the username is unknown or the password is wrong.
 * Carries the same generic message in both cases so the API never reveals which field was
 * incorrect. Mapped to HTTP 401.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
