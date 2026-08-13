package com.travelmemory.dna.controller;

import com.travelmemory.config.SecurityConfig;
import com.travelmemory.dna.dto.TravelDnaScoresResponse;
import com.travelmemory.dna.dto.TripDnaResponse;
import com.travelmemory.dna.service.TravelDnaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripDnaController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class TripDnaControllerWebTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TravelDnaService travelDnaService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void anonymousUserCannotReadTripDna() throws Exception {
        UUID tripId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/trips/{tripId}/dna", tripId))
                .andExpect(status().isUnauthorized());

        verify(travelDnaService, never()).forTrip(tripId);
    }

    @Test
    void authenticatedViewerReceivesCalculatedTripDna() throws Exception {
        UUID tripId = UUID.randomUUID();
        when(travelDnaService.forTrip(tripId)).thenReturn(new TripDnaResponse(
                tripId,
                new TravelDnaScoresResponse(82, 71, 64, 20, 34, 41, 57),
                "EXPLORER",
                List.of("8 saved places across 5 days")));

        mockMvc.perform(get("/api/v1/trips/{tripId}/dna", tripId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dominantTrait").value("EXPLORER"))
                .andExpect(jsonPath("$.scores.explorer").value(82))
                .andExpect(jsonPath("$.signals[0]").value("8 saved places across 5 days"));
    }
}
