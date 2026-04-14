package com.rideflow.uberclone;

import com.rideflow.uberclone.auth.security.AuthenticatedUser;
import com.rideflow.uberclone.auth.security.JwtService;
import com.rideflow.uberclone.user.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local")
class StaleJwtRequestTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void staleTokenShouldNotCrashRequestPipeline() throws Exception {
        AuthenticatedUser missingUser = new AuthenticatedUser(
                UUID.randomUUID(),
                "+994500009999",
                "ignored",
                Role.DRIVER
        );
        String token = jwtService.generateToken(missingUser);

        mockMvc.perform(get("/drivers/me/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
