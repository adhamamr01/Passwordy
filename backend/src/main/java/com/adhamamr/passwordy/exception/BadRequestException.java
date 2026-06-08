package com.adhamamr.passwordy.exception;

/**
 * Thrown for invalid client input or state that isn't caught by Bean Validation — e.g. a
 * master password that fails strength rules, or a duplicate username/email. Mapped to HTTP 400.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
