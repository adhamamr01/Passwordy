package com.adhamamr.passwordy.controller;

import com.adhamamr.passwordy.dto.PasswordResponse;
import com.adhamamr.passwordy.exception.ResourceNotFoundException;
import com.adhamamr.passwordy.exception.UnauthorizedException;
import com.adhamamr.passwordy.security.JwtUtil;
import com.adhamamr.passwordy.service.PasswordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "ratelimit.enabled=false")
class PasswordControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtUtil jwtUtil;
    @MockitoBean PasswordService passwordService;
    @MockitoBean UserDetailsService userDetailsService;

    private String bearerToken;

    private static final PasswordResponse SAMPLE = new PasswordResponse(
            1L, "Gmail", "encrypted", "alice@gmail.com",
            "https://gmail.com", null, "Email", Instant.now(), Instant.now());

    @BeforeEach
    void setUp() {
        bearerToken = "Bearer " + jwtUtil.generateToken("alice");
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(User.withUsername("alice").password("$hashed$").roles().build());
    }

    @Test
    void generatePassword_noAuth_returns200() throws Exception {
        when(passwordService.generatePassword(16, true)).thenReturn("Str0ng@Pass");

        mockMvc.perform(post("/api/password/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("length", 16, "includeSymbols", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").value("Str0ng@Pass"));
    }

    @Test
    void generatePin_noAuth_returns200() throws Exception {
        when(passwordService.generatePin(6)).thenReturn("123456");

        mockMvc.perform(post("/api/password/generate-pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("length", 6))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pin").value("123456"));
    }

    @Test
    void getPasswords_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/passwords"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPasswords_withToken_returnsPasswordList() throws Exception {
        when(passwordService.getAllPasswords("alice")).thenReturn(List.of(SAMPLE));

        mockMvc.perform(get("/api/passwords")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("Gmail"));
    }

    @Test
    void savePassword_withToken_returns201() throws Exception {
        when(passwordService.savePassword(any(), eq("alice"))).thenReturn(SAMPLE);

        mockMvc.perform(post("/api/passwords")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "label", "Gmail",
                                "password", "secret",
                                "username", "alice@gmail.com",
                                "url", "https://gmail.com",
                                "category", "Email"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("Gmail"));
    }

    @Test
    void getPasswordById_otherUser_returns403() throws Exception {
        when(passwordService.getPasswordById(eq(1L), eq("alice")))
                .thenThrow(new UnauthorizedException("Unauthorized access to password"));

        mockMvc.perform(get("/api/passwords/1")
                        .header("Authorization", bearerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Unauthorized access to password"));
    }

    @Test
    void getPasswordById_notFound_returns404() throws Exception {
        when(passwordService.getPasswordById(eq(99L), eq("alice")))
                .thenThrow(new ResourceNotFoundException("Password not found with id: 99"));

        mockMvc.perform(get("/api/passwords/99")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePassword_withToken_returns204() throws Exception {
        mockMvc.perform(delete("/api/passwords/1")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void decryptPassword_withToken_returnsPlaintext() throws Exception {
        when(passwordService.decryptPassword(eq(1L), eq("alice"))).thenReturn("secret");

        mockMvc.perform(post("/api/passwords/1/decrypt")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").value("secret"));
    }
}
