package com.adhamamr.passwordy.controller;

import com.adhamamr.passwordy.dto.AuthResponse;
import com.adhamamr.passwordy.dto.MessageResponse;
import com.adhamamr.passwordy.exception.BadRequestException;
import com.adhamamr.passwordy.exception.EmailNotVerifiedException;
import com.adhamamr.passwordy.exception.InvalidCredentialsException;
import com.adhamamr.passwordy.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "ratelimit.enabled=false")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean AuthService authService;

    @Test
    void register_validRequest_returns202WithGenericMessageAndNoToken() throws Exception {
        when(authService.register(any())).thenReturn(new MessageResponse("verification link sent"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice",
                                "email", "alice@example.com",
                                "masterPassword", "StrongP@ss1"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("verification link sent"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void register_missingField_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice",
                                "email", "alice@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        when(authService.register(any())).thenThrow(new BadRequestException("Password must be at least 8 characters long"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice",
                                "email", "alice@example.com",
                                "masterPassword", "weak"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verify_validToken_returns200WithMessage() throws Exception {
        when(authService.verify(eq("tok-123"))).thenReturn(new MessageResponse("Email verified. You can now log in."));

        mockMvc.perform(get("/api/auth/verify").param("token", "tok-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified. You can now log in."));
    }

    @Test
    void verify_invalidToken_returns400() throws Exception {
        when(authService.verify(any())).thenThrow(new BadRequestException("Invalid or expired verification token"));

        mockMvc.perform(get("/api/auth/verify").param("token", "bad"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_returns200WithAccessAndRefreshTokens() throws Exception {
        when(authService.login(any())).thenReturn(
                new AuthResponse("jwt-token", "refresh-tok", "alice", "alice@example.com", "Login successful", false, null));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice",
                                "masterPassword", "StrongP@ss1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-tok"));
    }

    @Test
    void refresh_validToken_returns200WithNewTokens() throws Exception {
        when(authService.refresh(any())).thenReturn(
                new AuthResponse("new-access", "new-refresh", "alice", "alice@example.com", "Token refreshed", false, null));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "old-refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void refresh_invalidToken_returns401() throws Exception {
        when(authService.refresh(any())).thenThrow(new InvalidCredentialsException("Invalid or expired refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "bad"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_returns200() throws Exception {
        when(authService.logout(any())).thenReturn(new MessageResponse("Logged out."));

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "rt"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out."));
    }

    @Test
    void login_wrongCredentials_returns401() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice",
                                "masterPassword", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    void forgotPassword_returns202WithGenericMessage() throws Exception {
        when(authService.forgotPassword(any())).thenReturn(new MessageResponse("If an account with that email exists, we've sent it an email."));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "alice@example.com"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void resetPassword_validToken_returns200() throws Exception {
        when(authService.resetPassword(any())).thenReturn(new MessageResponse("Your master password has been reset. You can now log in."));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", "rst-123", "newPassword", "NewStr0ng@1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your master password has been reset. You can now log in."));
    }

    @Test
    void resetPassword_invalidToken_returns400() throws Exception {
        when(authService.resetPassword(any())).thenThrow(new BadRequestException("Invalid or expired token"));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", "bad", "newPassword", "NewStr0ng@1"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_unverifiedAccount_returns403() throws Exception {
        when(authService.login(any())).thenThrow(new EmailNotVerifiedException("Please verify your email before logging in"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice",
                                "masterPassword", "StrongP@ss1"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Please verify your email before logging in"));
    }
}
