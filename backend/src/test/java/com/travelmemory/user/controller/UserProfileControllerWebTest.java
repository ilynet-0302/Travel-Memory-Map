package com.travelmemory.user.controller;

import com.travelmemory.config.SecurityConfig;
import com.travelmemory.statistics.dto.TravelStatisticsResponse;
import com.travelmemory.user.dto.UpdateUserProfileRequest;
import com.travelmemory.user.dto.UserProfileResponse;
import com.travelmemory.user.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserProfileController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class UserProfileControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void anonymousRequestCannotReadProfile() throws Exception {
        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isUnauthorized());

        verify(userProfileService, never()).getCurrentProfile();
    }

    @Test
    void authenticatedUserCanReadAndUpdateProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileResponse profile = profileResponse(userId, "Iliya Petrov");
        when(userProfileService.getCurrentProfile()).thenReturn(profile);
        when(userProfileService.updateCurrentProfile(any(UpdateUserProfileRequest.class))).thenReturn(profile);

        mockMvc.perform(get("/api/v1/profile").with(jwt().jwt(token -> token.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Iliya Petrov"))
                .andExpect(jsonPath("$.statistics.countriesVisited").value(3));

        mockMvc.perform(put("/api/v1/profile")
                        .with(jwt().jwt(token -> token.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Iliya Petrov"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Iliya Petrov"));
    }

    @Test
    void invalidDisplayNameIsRejectedBeforeServiceCall() throws Exception {
        mockMvc.perform(put("/api/v1/profile")
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":" "}
                                """))
                .andExpect(status().isBadRequest());

        verify(userProfileService, never()).updateCurrentProfile(any());
    }

    private UserProfileResponse profileResponse(UUID userId, String displayName) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new UserProfileResponse(
                userId,
                "ilia@example.com",
                displayName,
                null,
                now.minusYears(1),
                now,
                new TravelStatisticsResponse(3, 5, 4, 3, 18, 24));
    }
}
