package com.travelmemory.replay.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.photo.entity.Photo;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.photo.service.PhotoStorageService;
import com.travelmemory.replay.dto.TripReplayResponse;
import com.travelmemory.replay.dto.ReplayRouteSource;
import com.travelmemory.trip.entity.StopCategory;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStop;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.repository.TripStopRepository;
import com.travelmemory.trip.service.TripService;
import com.travelmemory.user.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TripReplayServiceTest {

    private TripService tripService;
    private TripStopRepository stopRepository;
    private PhotoRepository photoRepository;
    private TripPermissionService permissionService;
    private AuthenticatedUserProvider userProvider;
    private PhotoStorageService storageService;
    private RoadRouteService roadRouteService;
    private TripReplayService replayService;
    private UserProfile owner;
    private Trip trip;

    @BeforeEach
    void setUp() {
        tripService = mock(TripService.class);
        stopRepository = mock(TripStopRepository.class);
        photoRepository = mock(PhotoRepository.class);
        permissionService = mock(TripPermissionService.class);
        userProvider = mock(AuthenticatedUserProvider.class);
        storageService = mock(PhotoStorageService.class);
        roadRouteService = mock(RoadRouteService.class);
        replayService = new TripReplayService(
                tripService, stopRepository, photoRepository, permissionService, userProvider, storageService, roadRouteService);
        owner = new UserProfile(UUID.randomUUID(), "owner@example.com", "Owner");
        trip = new Trip(
                owner, "Roman Holiday", null, "Italy", "IT", "Rome",
                LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 16), TripVisibility.PRIVATE);
        when(tripService.getTripEntity(trip.getId())).thenReturn(trip);
        when(userProvider.getCurrentUser()).thenReturn(new AuthenticatedUser(owner.getId(), owner.getEmail(), owner.getDisplayName()));
        when(userProvider.getCurrentAccessToken()).thenReturn("access-token");
    }

    @Test
    void buildsOrderedFramesDistanceAndStopPhotos() {
        TripStop airport = stop("Airport", 41.8000, 12.2500, 0, 9);
        TripStop hotel = stop("Hotel", 41.9000, 12.5000, 1, 11);
        Photo photo = new Photo(
                UUID.randomUUID(), trip, hotel, owner, trip.getId() + "/hotel.jpg", "hotel.jpg", "image/jpeg",
                128, hotel.getArrivalTime(), null, null, "We made it");
        when(stopRepository.findByTripIdOrderByPositionAsc(trip.getId())).thenReturn(List.of(airport, hotel));
        when(photoRepository.findByTripIdOrderByTakenAtAscCreatedAtAsc(trip.getId())).thenReturn(List.of(photo));
        when(storageService.createSignedUrl(photo.getStoragePath(), "access-token")).thenReturn("https://signed.example/hotel");
        when(roadRouteService.calculate(List.of(airport, hotel))).thenReturn(new RoadRouteResult(
                ReplayRouteSource.ROUTED, "driving", new BigDecimal("28.4"), 1_420, List.of()));

        TripReplayResponse replay = replayService.buildReplay(trip.getId());

        assertThat(replay.frames()).extracting(frame -> frame.name()).containsExactly("Airport", "Hotel");
        assertThat(replay.frames().get(1).photos()).singleElement().satisfies(memory -> {
            assertThat(memory.signedUrl()).isEqualTo("https://signed.example/hotel");
            assertThat(memory.caption()).isEqualTo("We made it");
        });
        assertThat(replay.totalDistanceKm()).isEqualByComparingTo("28.4");
        assertThat(replay.estimatedDurationSeconds()).isEqualTo(3);
        assertThat(replay.travelDurationSeconds()).isEqualTo(1_420);
        assertThat(replay.routeSource()).isEqualTo(ReplayRouteSource.ROUTED);
        verify(permissionService).requireViewAccess(trip, owner.getId());
    }

    @Test
    void ignoresWholeTripPhotosWithoutAStopFrame() {
        TripStop stop = stop("Colosseum", 41.8902, 12.4922, 0, 14);
        Photo wholeTripPhoto = new Photo(
                UUID.randomUUID(), trip, null, owner, trip.getId() + "/cover.jpg", "cover.jpg", "image/jpeg",
                128, null, null, null, "Cover");
        when(stopRepository.findByTripIdOrderByPositionAsc(trip.getId())).thenReturn(List.of(stop));
        when(photoRepository.findByTripIdOrderByTakenAtAscCreatedAtAsc(trip.getId())).thenReturn(List.of(wholeTripPhoto));
        when(roadRouteService.calculate(List.of(stop))).thenReturn(new RoadRouteResult(
                ReplayRouteSource.NO_ROUTE, "driving", new BigDecimal("0.0"), 0, List.of()));

        TripReplayResponse replay = replayService.buildReplay(trip.getId());

        assertThat(replay.frames().getFirst().photos()).isEmpty();
        assertThat(replay.totalDistanceKm()).isEqualByComparingTo("0.0");
    }

    private TripStop stop(String name, double latitude, double longitude, int position, int hour) {
        return new TripStop(
                trip,
                owner,
                name,
                null,
                BigDecimal.valueOf(latitude),
                BigDecimal.valueOf(longitude),
                OffsetDateTime.of(2026, 9, 12 + position, hour, 0, 0, 0, ZoneOffset.UTC),
                null,
                StopCategory.LANDMARK,
                null,
                position);
    }
}
