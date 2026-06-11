package com.adhamamr.passwordy.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Issues and validates the HS256 JSON Web Tokens that authenticate API requests.
 *
 * <p>A token carries the username as its subject and expires after the configured validity.
 * Because auth is stateless, the token itself is the only proof of identity — there is no
 * server-side session to revoke before expiry.
 *
 * <p>The signing key ({@code jwt.secret}, Base64-encoded) and validity ({@code jwt.expiration},
 * millis) come from configuration. The committed profile ships throwaway dev defaults; real
 * deployments override them via the gitignored {@code application-{local,docker}.properties}.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long tokenValidityMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long tokenValidityMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.tokenValidityMs = tokenValidityMs;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /** Mints a short-lived access token; subject is the username, expiry from {@code jwt.expiration}. */
    public String generateToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + tokenValidityMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Returns true only if the token's subject matches {@code username} and the token has
     * not expired. Callers pass the username resolved from their own user store, so a token
     * signed for a different (or deleted) user is rejected.
     */
    public boolean validateToken(String token, String username) {
        return extractUsername(token).equals(username) && !isTokenExpired(token);
    }
}
