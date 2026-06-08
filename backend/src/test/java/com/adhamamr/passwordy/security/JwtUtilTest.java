package com.adhamamr.passwordy.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
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
