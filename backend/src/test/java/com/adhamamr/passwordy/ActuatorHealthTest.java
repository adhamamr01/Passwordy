package com.adhamamr.passwordy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Actuator health endpoint must be reachable without authentication (load balancers and
 * uptime monitors poll it), and it must report UP for a healthy context.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorHealthTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void healthEndpoint_isPublicAndReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
