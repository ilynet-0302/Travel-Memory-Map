package com.travelmemory.replay.controller;

import com.travelmemory.config.SecurityConfig;
import com.travelmemory.replay.dto.TripReplayResponse;
import com.travelmemory.replay.dto.ReplayRouteSource;
import com.travelmemory.replay.service.TripReplayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripReplayController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class TripReplayControllerWebTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TripReplayService tripReplayService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void replayRequiresAuthentication() throws Exception {
        UUID tripId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/trips/{tripId}/replay", tripId))
                .andExpect(status().isUnauthorized());

        verify(tripReplayService, never()).buildReplay(tripId);
    }

    @Test
    void authenticatedMemberReceivesDerivedReplay() throws Exception {
        UUID tripId = UUID.randomUUID();
        when(tripReplayService.buildReplay(tripId)).thenReturn(new TripReplayResponse(
                tripId, "Rome", LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 16),
                new BigDecimal("48.2"), 12, 3_840, ReplayRouteSource.ROUTED, "driving", List.of(), List.of()));

        mockMvc.perform(get("/api/v1/trips/{tripId}/replay", tripId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(tripId.toString()))
                .andExpect(jsonPath("$.totalDistanceKm").value(48.2))
                .andExpect(jsonPath("$.routeSource").value("ROUTED"));
    }
}
