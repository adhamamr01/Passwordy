package com.adhamamr.passwordy.ratelimit;

import com.adhamamr.passwordy.dto.AuthResponse;
import com.adhamamr.passwordy.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies the rate-limit filter trips at the configured capacity. Limits are shrunk via
 * properties so the threshold is reached in a few requests. {@link DirtiesContext} rebuilds
 * the context (and thus the in-memory bucket store) per method so the tiers start fresh.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "ratelimit.enabled=true",
        "ratelimit.auth.capacity=3",
        "ratelimit.auth.refill-seconds=60",
        "ratelimit.generation.capacity=3",
        "ratelimit.generation.refill-seconds=60"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RateLimitIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean AuthService authService;

    @Test
    void authTier_blocksAfterCapacity() throws Exception {
        when(authService.login(any())).thenReturn(
                new AuthResponse("token", "refresh", "alice", "alice@example.com", "Login successful", false, null));

        String body = objectMapper.writeValueAsString(Map.of(
                "username", "alice", "masterPassword", "StrongP@ss1"));

        // capacity = 3: first three pass
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }

        // fourth is throttled
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("Too many requests, please try again later"));
    }

    @Test
    void generationTier_blocksAfterCapacity() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("length", 16, "includeSymbols", true));

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/password/generate")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/password/generate")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
    }
}
