package com.adhamamr.passwordy.service;

import com.adhamamr.passwordy.dto.AuthResponse;
import com.adhamamr.passwordy.dto.LoginRequest;
import com.adhamamr.passwordy.dto.RegisterRequest;
import com.adhamamr.passwordy.exception.BadRequestException;
import com.adhamamr.passwordy.exception.InvalidCredentialsException;
import com.adhamamr.passwordy.model.User;
import com.adhamamr.passwordy.repository.UserRepository;
import com.adhamamr.passwordy.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("$hashed$");
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtUtil);
    }

    // --- register ---

    @Test
    void register_validRequest_returnsTokenAndSavesUser() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        User saved = new User("alice", "alice@example.com", "$hashed$");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtUtil.generateToken("alice")).thenReturn("jwt-token");

        AuthResponse response = authService.register(
                new RegisterRequest("alice", "alice@example.com", "StrongP@ss1"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("alice");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_weakPassword_throwsBadRequest() {
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alice", "alice@example.com", "weak")))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateUsername_throwsBadRequest() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alice", "alice@example.com", "StrongP@ss1")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username");
    }

    @Test
    void register_duplicateEmail_throwsBadRequest() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alice", "alice@example.com", "StrongP@ss1")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email");
    }

    // --- login ---

    @Test
    void login_validCredentials_returnsToken() {
        User user = new User("alice", "alice@example.com", "$hashed$");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongP@ss1", "$hashed$")).thenReturn(true);
        when(passwordEncoder.upgradeEncoding("$hashed$")).thenReturn(false);
        when(jwtUtil.generateToken("alice")).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("alice", "StrongP@ss1"));

        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void login_unknownUsername_throwsInvalidCredentials() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "any")))
                .isInstanceOf(InvalidCredentialsException.class);
        // timing guard: encoder must still be called
        verify(passwordEncoder).matches(eq("any"), anyString());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        User user = new User("alice", "alice@example.com", "$hashed$");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$hashed$")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_outdatedHash_triggersRehash() {
        User user = new User("alice", "alice@example.com", "$bcrypt$");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongP@ss1", "$bcrypt$")).thenReturn(true);
        when(passwordEncoder.upgradeEncoding("$bcrypt$")).thenReturn(true);
        when(passwordEncoder.encode("StrongP@ss1")).thenReturn("$argon2$");
        when(jwtUtil.generateToken("alice")).thenReturn("jwt-token");

        authService.login(new LoginRequest("alice", "StrongP@ss1"));

        verify(userRepository).save(argThat(u -> u.getMasterPasswordHash().equals("$argon2$")));
    }
}
