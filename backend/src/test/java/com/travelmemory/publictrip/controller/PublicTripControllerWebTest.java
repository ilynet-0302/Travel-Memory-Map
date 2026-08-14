package com.travelmemory.publictrip.controller;

import com.travelmemory.config.SecurityConfig;
import com.travelmemory.publictrip.dto.PublicTripRatingResponse;
import com.travelmemory.publictrip.dto.PublicTripResponse;
import com.travelmemory.publictrip.dto.PublicTripStatisticsResponse;
import com.travelmemory.publictrip.service.PublicTripService;
import com.travelmemory.rating.dto.ReturnIntentSummaryResponse;
import com.travelmemory.rating.dto.TripRatingBreakdownResponse;
import com.travelmemory.replay.dto.ReplayRouteSource;
import com.travelmemory.trip.entity.TripStatus;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicTripController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class PublicTripControllerWebTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PublicTripService publicTripService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    void anonymousVisitorCanReadOnlyTheCuratedPublicStory() throws Exception {
        String slug = "corfu-in-blue";
        when(publicTripService.getBySlug(slug)).thenReturn(new PublicTripResponse(
                slug, "Corfu in Blue", "Saltwater afternoons", "Greece", "GR", "Corfu",
                LocalDate.of(2025, 8, 17), LocalDate.of(2025, 8, 23), TripStatus.COMPLETED, null,
                new PublicTripStatisticsResponse(7, 8, 3, new BigDecimal("84.2")),
                ReplayRouteSource.ROUTED, List.of(), List.of(), List.of(),
                new PublicTripRatingResponse(
                        new BigDecimal("9.1"), 4,
                        new TripRatingBreakdownResponse(null, null, null, null, null, null, null, null),
                        new ReturnIntentSummaryResponse(3, 1, 0))));

        mockMvc.perform(get("/api/v1/public/trips/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicSlug").value(slug))
                .andExpect(jsonPath("$.title").value("Corfu in Blue"))
                .andExpect(jsonPath("$.statistics.travelDays").value(7))
                .andExpect(jsonPath("$.rating.averageScore").value(9.1))
                .andExpect(jsonPath("$.ownerId").doesNotExist())
                .andExpect(jsonPath("$.memberCount").doesNotExist())
                .andExpect(jsonPath("$.expenses").doesNotExist())
                .andExpect(jsonPath("$.permissions").doesNotExist());

        verify(publicTripService).getBySlug(slug);
    }
}
