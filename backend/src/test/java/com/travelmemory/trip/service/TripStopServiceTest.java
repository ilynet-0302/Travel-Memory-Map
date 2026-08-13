package com.travelmemory.trip.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.exception.InvalidTripDateException;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.trip.dto.TripStopResponse;
import com.travelmemory.trip.dto.UpdateTripStopRequest;
import com.travelmemory.trip.entity.StopCategory;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStop;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.mapper.TripStopMapper;
import com.travelmemory.trip.repository.TripStopRepository;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TripStopServiceTest {

    private TripStopRepository stopRepository;
    private TripPermissionService permissionService;
    private AuthenticatedUserProvider userProvider;
    private TripStopMapper stopMapper;
    private TripStopService service;
    private Trip trip;
    private TripStop stop;
    private AuthenticatedUser editorIdentity;

    @BeforeEach
    void setUp() {
        TripService tripService = mock(TripService.class);
        stopRepository = mock(TripStopRepository.class);
        permissionService = mock(TripPermissionService.class);
        userProvider = mock(AuthenticatedUserProvider.class);
        UserProfileService profileService = mock(UserProfileService.class);
        stopMapper = mock(TripStopMapper.class);
        service = new TripStopService(
                tripService, stopRepository, permissionService, userProvider, profileService, stopMapper);

        UserProfile owner = new UserProfile(UUID.randomUUID(), "owner@example.com", "Owner");
        UserProfile editor = new UserProfile(UUID.randomUUID(), "editor@example.com", "Editor");
        editorIdentity = new AuthenticatedUser(editor.getId(), editor.getEmail(), editor.getDisplayName());
        trip = new Trip(
                owner, "Rome", null, "Italy", "IT", "Rome",
                LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 16), TripVisibility.PRIVATE);
        stop = new TripStop(
                trip,
                editor,
                "Colosseum",
                "Ancient amphitheatre",
                new BigDecimal("41.890200"),
                new BigDecimal("12.492200"),
                time(2026, 9, 12, 14, 0),
                null,
                StopCategory.LANDMARK,
                9,
                0);
        when(tripService.getTripEntity(trip.getId())).thenReturn(trip);
        when(userProvider.getCurrentUser()).thenReturn(editorIdentity);
    }

    @Test
    void editorCanUpdateStopInsideTrip() {
        UpdateTripStopRequest request = new UpdateTripStopRequest(
                "Roman Forum",
                "Sunset walk",
                new BigDecimal("41.892500"),
                new BigDecimal("12.485300"),
                time(2026, 9, 13, 17, 30),
                time(2026, 9, 13, 19, 0),
                StopCategory.LANDMARK,
                10,
                2);
        TripStopResponse mapped = mock(TripStopResponse.class);
        when(stopRepository.findByIdAndTripId(stop.getId(), trip.getId())).thenReturn(Optional.of(stop));
        when(stopMapper.toResponse(stop)).thenReturn(mapped);

        TripStopResponse response = service.updateStop(trip.getId(), stop.getId(), request);

        assertThat(response).isSameAs(mapped);
        assertThat(stop.getName()).isEqualTo("Roman Forum");
        assertThat(stop.getDescription()).isEqualTo("Sunset walk");
        assertThat(stop.getPosition()).isEqualTo(2);
        verify(permissionService).requireEditorOrOwner(trip, editorIdentity.id());
    }

    @Test
    void deleteUsesTripScopedStopLookup() {
        when(stopRepository.findByIdAndTripId(stop.getId(), trip.getId())).thenReturn(Optional.of(stop));

        service.deleteStop(trip.getId(), stop.getId());

        verify(permissionService).requireEditorOrOwner(trip, editorIdentity.id());
        verify(stopRepository).delete(stop);
    }

    @Test
    void updateRejectsDepartureBeforeArrival() {
        UpdateTripStopRequest request = new UpdateTripStopRequest(
                "Colosseum",
                null,
                new BigDecimal("41.890200"),
                new BigDecimal("12.492200"),
                time(2026, 9, 13, 14, 0),
                time(2026, 9, 13, 13, 0),
                StopCategory.LANDMARK,
                null,
                0);

        assertThatThrownBy(() -> service.updateStop(trip.getId(), stop.getId(), request))
                .isInstanceOf(InvalidTripDateException.class);
        verify(stopRepository, never()).findByIdAndTripId(stop.getId(), trip.getId());
    }

    private OffsetDateTime time(int year, int month, int day, int hour, int minute) {
        return OffsetDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.UTC);
    }
}
