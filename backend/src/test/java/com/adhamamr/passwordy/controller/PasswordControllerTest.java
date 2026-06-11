package com.adhamamr.passwordy.controller;

import com.adhamamr.passwordy.dto.PasswordResponse;
import com.adhamamr.passwordy.exception.ResourceNotFoundException;
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
import static org.mockito.Mockito.verify;
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
            "https://gmail.com", null, "Email", false, Instant.now(), Instant.now());

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
    void getPasswordById_otherUser_returns404() throws Exception {
        // A password owned by another user is indistinguishable from a non-existent id:
        // both surface as 404 so the route can't be used to enumerate ids across users.
        when(passwordService.getPasswordById(eq(1L), eq("alice")))
                .thenThrow(new ResourceNotFoundException("Password not found with id: 1"));

        mockMvc.perform(get("/api/passwords/1")
                        .header("Authorization", bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Password not found with id: 1"));
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

    // --- favorites ---

    @Test
    void setFavorite_withToken_returns200WithFavoriteTrue() throws Exception {
        PasswordResponse favorited = new PasswordResponse(
                1L, "Gmail", "encrypted", "alice@gmail.com",
                "https://gmail.com", null, "Email", true, Instant.now(), Instant.now());
        when(passwordService.setFavorite(eq(1L), eq(true), eq("alice"))).thenReturn(favorited);

        mockMvc.perform(put("/api/passwords/1/favorite")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("favorite", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(true));
    }

    @Test
    void setFavorite_otherUsersPassword_returns404() throws Exception {
        when(passwordService.setFavorite(eq(1L), eq(true), eq("alice")))
                .thenThrow(new ResourceNotFoundException("Password not found with id: 1"));

        mockMvc.perform(put("/api/passwords/1/favorite")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("favorite", true))))
                .andExpect(status().isNotFound());
    }

    @Test
    void setFavorite_missingField_returns400() throws Exception {
        mockMvc.perform(put("/api/passwords/1/favorite")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setFavorite_noToken_returns401() throws Exception {
        mockMvc.perform(put("/api/passwords/1/favorite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("favorite", true))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPasswords_favoritesOnly_returnsFilteredList() throws Exception {
        PasswordResponse favorited = new PasswordResponse(
                1L, "Gmail", "encrypted", "alice@gmail.com",
                "https://gmail.com", null, "Email", true, Instant.now(), Instant.now());
        when(passwordService.getFavorites("alice")).thenReturn(List.of(favorited));

        mockMvc.perform(get("/api/passwords?favoritesOnly=true")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].favorite").value(true));
        verify(passwordService).getFavorites("alice");
    }
}
