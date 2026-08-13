package com.travelmemory.config;

import com.travelmemory.invitation.controller.InvitationController;
import com.travelmemory.invitation.dto.AcceptInviteResponse;
import com.travelmemory.invitation.dto.InvitePreviewResponse;
import com.travelmemory.invitation.service.TripInvitationService;
import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.membership.service.TripMemberService;
import com.travelmemory.trip.controller.TripController;
import com.travelmemory.trip.dto.CreateTripRequest;
import com.travelmemory.trip.dto.TripDetailsResponse;
import com.travelmemory.trip.entity.TripStatus;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.service.TripService;
import com.travelmemory.trip.service.TripStopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {TripController.class, InvitationController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class ApiSecurityWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private TripStopService tripStopService;

    @MockitoBean
    private TripMemberService tripMemberService;

    @MockitoBean
    private TripInvitationService invitationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void protectedTripEndpointRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/trips"))
                .andExpect(status().isUnauthorized());

        verify(tripService, never()).listTrips();
    }

    @Test
    void anonymousVisitorCanPreviewInviteButCannotAcceptIt() throws Exception {
        String token = "secure-invite-token";
        UUID tripId = UUID.randomUUID();
        when(invitationService.previewInvite(token)).thenReturn(new InvitePreviewResponse(
                tripId,
                "Italy Road Trip",
                "Italy",
                "Rome",
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 18),
                "Iliya",
                TripRole.EDITOR,
                OffsetDateTime.of(2026, 8, 20, 12, 0, 0, 0, ZoneOffset.UTC)));

        mockMvc.perform(get("/api/v1/invites/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(tripId.toString()))
                .andExpect(jsonPath("$.role").value("EDITOR"));

        mockMvc.perform(post("/api/v1/invites/{token}/accept", token))
                .andExpect(status().isUnauthorized());
        verify(invitationService, never()).acceptInvite(token);
    }

    @Test
    void validJwtCanCreateTripAndAcceptInvite() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        String token = "secure-invite-token";
        when(tripService.createTrip(any(CreateTripRequest.class))).thenReturn(new TripDetailsResponse(
                tripId,
                userId,
                "Italy Road Trip",
                "A shared journey",
                "Italy",
                "IT",
                "Rome",
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 18),
                TripStatus.UPCOMING,
                TripVisibility.PRIVATE,
                null,
                TripRole.OWNER,
                1,
                List.of()));
        when(invitationService.acceptInvite(token)).thenReturn(
                new AcceptInviteResponse(tripId, UUID.randomUUID(), TripRole.EDITOR));

        var authenticatedJwt = jwt().jwt(jwt -> jwt
                .subject(userId.toString())
                .claim("email", "owner@example.com")
                .claim("user_metadata", Map.of("full_name", "Trip Owner")));

        mockMvc.perform(post("/api/v1/trips")
                        .with(authenticatedJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Italy Road Trip",
                                  "description": "A shared journey",
                                  "country": "Italy",
                                  "countryCode": "IT",
                                  "city": "Rome",
                                  "startDate": "2026-09-12",
                                  "endDate": "2026-09-18",
                                  "visibility": "PRIVATE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(tripId.toString()))
                .andExpect(jsonPath("$.currentUserRole").value("OWNER"));

        mockMvc.perform(post("/api/v1/invites/{token}/accept", token).with(authenticatedJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EDITOR"));
    }

    @Test
    void authenticatedInvalidTripPayloadIsRejectedBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/v1/trips")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "country": "Italy",
                                  "city": "Rome",
                                  "startDate": "2026-09-12",
                                  "endDate": "2026-09-18",
                                  "visibility": "PRIVATE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(tripService, never()).createTrip(any(CreateTripRequest.class));
    }
}
