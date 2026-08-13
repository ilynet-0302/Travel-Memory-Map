package com.travelmemory.rating.controller;

import com.travelmemory.config.SecurityConfig;
import com.travelmemory.rating.dto.ReturnIntentSummaryResponse;
import com.travelmemory.rating.dto.TripRatingBreakdownResponse;
import com.travelmemory.rating.dto.TripRatingResponse;
import com.travelmemory.rating.dto.TripRatingSummaryResponse;
import com.travelmemory.rating.dto.UpsertTripRatingRequest;
import com.travelmemory.rating.entity.WouldReturn;
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
        UpsertTripRatingRequest request = new UpsertTripRatingRequest(
                9, 7, 10, 8, 8, 8, 6, 9, WouldReturn.YES);
        when(tripRatingService.rate(tripId, request))
                .thenReturn(new TripRatingSummaryResponse(
                        new BigDecimal("8.7"),
                        3,
                        new TripRatingResponse(8, 9, 7, 10, 8, 8, 8, 6, 9, WouldReturn.YES),
                        new TripRatingBreakdownResponse(
                                new BigDecimal("8.3"), new BigDecimal("7.7"), new BigDecimal("9.3"),
                                new BigDecimal("8.0"), new BigDecimal("8.7"), new BigDecimal("8.0"),
                                new BigDecimal("6.7"), new BigDecimal("8.7")),
                        new ReturnIntentSummaryResponse(2, 1, 0),
                        true));

        mockMvc.perform(put("/api/v1/trips/{tripId}/rating", tripId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson(9)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageScore").value(8.7))
                .andExpect(jsonPath("$.ratingCount").value(3))
                .andExpect(jsonPath("$.currentUserRating.overallScore").value(8))
                .andExpect(jsonPath("$.currentUserRating.culture").value(10))
                .andExpect(jsonPath("$.averages.food").value(8.3))
                .andExpect(jsonPath("$.returnIntent.yes").value(2))
                .andExpect(jsonPath("$.canRate").value(true));
    }

    @Test
    void dimensionOutsideTheOneToTenRangeIsRejectedBeforeTheService() throws Exception {
        UUID tripId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/trips/{tripId}/rating", tripId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson(11)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(tripRatingService, never()).rate(any(), any(UpsertTripRatingRequest.class));
    }

    private String validRequestJson(int food) {
        return """
                {
                  "food": %d,
                  "nightlife": 7,
                  "culture": 10,
                  "nature": 8,
                  "walkability": 8,
                  "valueForMoney": 8,
                  "crowds": 6,
                  "relaxation": 9,
                  "wouldReturn": "YES"
                }
                """.formatted(food);
    }
}
