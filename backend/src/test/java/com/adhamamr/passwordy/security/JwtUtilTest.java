package com.adhamamr.passwordy.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    // Base64-encoded 64-byte test key; value is irrelevant beyond being valid for HS256.
    private static final String TEST_SECRET =
            "aK55YKBmvk1ckwElf+VLQNod04L1IJNr2g1xYlNB6LL0p7pPx1ERvdwRXw81yvQtsZs4GmIx7552EO6LyDuTdQ==";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, 86400000L);
    }

    @Test
    void generatedToken_containsUsername() {
        String token = jwtUtil.generateToken("alice");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken("alice");
        assertThat(jwtUtil.validateToken(token, "alice")).isTrue();
    }

    @Test
    void validateToken_wrongUsername_returnsFalse() {
        String token = jwtUtil.generateToken("alice");
        assertThat(jwtUtil.validateToken(token, "bob")).isFalse();
    }

    @Test
    void validateToken_tamperedToken_throws() {
        String token = jwtUtil.generateToken("alice");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThatThrownBy(() -> jwtUtil.validateToken(tampered, "alice"));
    }
}
