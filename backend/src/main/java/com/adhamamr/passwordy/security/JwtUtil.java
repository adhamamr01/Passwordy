package com.adhamamr.passwordy.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Issues and validates the HS256 JSON Web Tokens that authenticate API requests.
 *
 * <p>A token carries the username as its subject and expires after
 * {@link #JWT_TOKEN_VALIDITY}. Because auth is stateless, the token itself is the only
 * proof of identity — there is no server-side session to revoke before expiry.
 */
@Component
public class JwtUtil {

    /**
     * HMAC signing key. Hardcoded for development only; production should load this from
     * configuration/secrets (see DECISIONS.md §6).
     */
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private static final long JWT_TOKEN_VALIDITY = 24 * 60 * 60 * 1000;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(getSigningKey())
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

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
