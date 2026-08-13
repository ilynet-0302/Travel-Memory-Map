package com.travelmemory.rating.controller;

import com.travelmemory.config.SecurityConfig;
import com.travelmemory.rating.dto.TripRatingSummaryResponse;
import com.travelmemory.rating.service.TripRatingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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

@WebMvcTest(TripRatingController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class TripRatingControllerWebTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TripRatingService tripRatingService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void anonymousUserCannotReadTripRatings() throws Exception {
        UUID tripId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/trips/{tripId}/rating", tripId))
                .andExpect(status().isUnauthorized());

        verify(tripRatingService, never()).summary(tripId);
    }

    @Test
    void authenticatedMemberCanRateATrip() throws Exception {
        UUID tripId = UUID.randomUUID();
        when(tripRatingService.rate(tripId, 9))
                .thenReturn(new TripRatingSummaryResponse(new BigDecimal("8.7"), 3, 9, true));

        mockMvc.perform(put("/api/v1/trips/{tripId}/rating", tripId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageScore").value(8.7))
                .andExpect(jsonPath("$.ratingCount").value(3))
                .andExpect(jsonPath("$.currentUserScore").value(9))
                .andExpect(jsonPath("$.canRate").value(true));
    }

    @Test
    void scoreOutsideTheOneToTenRangeIsRejectedBeforeTheService() throws Exception {
        UUID tripId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/trips/{tripId}/rating", tripId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":11}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(tripRatingService, never()).rate(any(), any(Integer.class));
    }
}
