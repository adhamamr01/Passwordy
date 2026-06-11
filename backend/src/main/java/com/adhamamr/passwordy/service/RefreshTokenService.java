package com.adhamamr.passwordy.service;

import com.adhamamr.passwordy.exception.InvalidCredentialsException;
import com.adhamamr.passwordy.model.RefreshToken;
import com.adhamamr.passwordy.model.User;
import com.adhamamr.passwordy.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Issues, rotates, and revokes opaque refresh tokens. The raw token (a random UUID) is returned
 * to the client; only its SHA-256 hash is persisted, so the stored value is useless if leaked.
 * Rotation is delete-on-use: {@link #rotate} consumes the presented token and the caller mints a
 * fresh pair, so a replayed (already-used) token is simply not found and rejected.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final Duration ttl;

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${auth.refresh-ttl-days:30}") long ttlDays) {
        this.repository = repository;
        this.ttl = Duration.ofDays(ttlDays);
    }

    /** Creates a new refresh token for {@code user} and returns the raw value (hash is stored). */
    @Transactional
    public String issue(User user) {
        String raw = UUID.randomUUID().toString();
        repository.save(new RefreshToken(hash(raw), user, Instant.now().plus(ttl)));
        return raw;
    }

    /** Consumes a valid refresh token (deleting it) and returns its owner, for rotation. */
    @Transactional
    public User rotate(String rawToken) {
        RefreshToken token = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));
        repository.delete(token);
        if (token.isExpired()) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }
        return token.getUser();
    }

    /** Revokes a single refresh token (logout); silently ignores an unknown token. */
    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(repository::delete);
    }

    /** Revokes every refresh token for {@code user} (e.g. on password reset) — ends all sessions. */
    @Transactional
    public void revokeAll(User user) {
        repository.deleteByUser(user);
    }

    private String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
