package com.travelmemory.comparison.controller;

import com.travelmemory.comparison.dto.TripComparisonResponse;
import com.travelmemory.comparison.service.TripComparisonService;
import com.travelmemory.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripComparisonController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class TripComparisonControllerWebTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TripComparisonService tripComparisonService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void anonymousUserCannotCompareTrips() throws Exception {
        UUID left = UUID.randomUUID();
        UUID right = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/trips/comparison")
                        .queryParam("leftTripId", left.toString())
                        .queryParam("rightTripId", right.toString()))
                .andExpect(status().isUnauthorized());

        verify(tripComparisonService, never()).compare(left, right);
    }

    @Test
    void authenticatedUserCanCompareTwoTrips() throws Exception {
        UUID left = UUID.randomUUID();
        UUID right = UUID.randomUUID();
        when(tripComparisonService.compare(left, right))
                .thenReturn(new TripComparisonResponse(null, null));

        mockMvc.perform(get("/api/v1/trips/comparison")
                        .with(jwt())
                        .queryParam("leftTripId", left.toString())
                        .queryParam("rightTripId", right.toString()))
                .andExpect(status().isOk());

        verify(tripComparisonService).compare(left, right);
    }
}
