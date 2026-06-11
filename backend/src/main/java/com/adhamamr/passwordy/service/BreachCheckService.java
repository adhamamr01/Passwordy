package com.adhamamr.passwordy.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Checks a password against Have I Been Pwned using k-anonymity: the password is SHA-1 hashed and
 * <em>only the first 5 hex chars</em> are sent to HIBP, which returns every breached hash suffix
 * sharing that prefix; the match is done locally. The password (and its full hash) never leave the
 * server. SHA-1 is used solely as HIBP's index — storage is still Argon2id.
 *
 * <p><b>Fail-open:</b> if HIBP can't be reached, {@link #isBreached} returns {@code false} so a
 * third-party outage never blocks registration or password reset.
 */
@Service
public class BreachCheckService {

    private final PwnedRangeClient rangeClient;
    private final boolean enabled;

    public BreachCheckService(PwnedRangeClient rangeClient,
                              @Value("${breachcheck.enabled:true}") boolean enabled) {
        this.rangeClient = rangeClient;
        this.enabled = enabled;
    }

    /** True only if the password is positively found in a breach corpus; false if disabled or unreachable. */
    public boolean isBreached(String password) {
        if (!enabled || password == null || password.isEmpty()) {
            return false;
        }
        String sha1 = sha1Hex(password).toUpperCase();
        String prefix = sha1.substring(0, 5);
        String suffix = sha1.substring(5);

        return rangeClient.fetchRange(prefix)
                .map(body -> suffixPresent(body, suffix))
                .orElse(false);  // fail-open: couldn't reach HIBP
    }

    /** Each line is {@code SUFFIX:count}; a match means the password appears in a breach. */
    private boolean suffixPresent(String rangeBody, String suffix) {
        for (String line : rangeBody.split("\\r?\\n")) {
            int colon = line.indexOf(':');
            String candidate = colon >= 0 ? line.substring(0, colon) : line;
            if (candidate.trim().equalsIgnoreCase(suffix)) {
                return true;
            }
        }
        return false;
    }

    private String sha1Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 unavailable", e);
        }
    }
}
