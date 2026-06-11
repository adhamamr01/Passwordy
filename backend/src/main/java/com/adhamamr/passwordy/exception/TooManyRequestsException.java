package com.adhamamr.passwordy.exception;

/**
 * Thrown when a caller exceeds an application-level rate limit enforced outside the
 * {@code RateLimitFilter} — currently the per-account login throttle. Mapped to HTTP 429
 * by the global handler. (The filter-level tiers return 429 directly, with a Retry-After
 * header, without raising this exception.)
 */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
