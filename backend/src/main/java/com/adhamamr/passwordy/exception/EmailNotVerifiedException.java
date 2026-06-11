package com.adhamamr.passwordy.exception;

/**
 * Thrown at login when the credentials are correct but the account's email has not been
 * verified yet. Raised only <em>after</em> a successful password check, so it never reveals
 * account existence to someone who doesn't already know the password. Mapped to HTTP 403.
 */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
