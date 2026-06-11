package com.adhamamr.passwordy.service;

import java.util.Optional;

/**
 * Fetches the Have I Been Pwned "range" result for a 5-char SHA-1 prefix — the only thing ever
 * sent to HIBP (k-anonymity). Returns the raw response body, or {@link Optional#empty()} when the
 * lookup couldn't complete (network/timeout/non-200) so callers can fail open.
 */
public interface PwnedRangeClient {
    Optional<String> fetchRange(String hashPrefix5);
}
