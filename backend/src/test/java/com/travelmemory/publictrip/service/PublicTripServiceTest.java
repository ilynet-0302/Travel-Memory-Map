package com.travelmemory.publictrip.service;

import com.travelmemory.exception.PublicTripNotFoundException;
import com.travelmemory.photo.entity.Photo;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.photo.service.PhotoStorageService;
import com.travelmemory.rating.entity.TripRating;
import com.travelmemory.rating.entity.WouldReturn;
import com.travelmemory.rating.repository.TripRatingRepository;
import com.travelmemory.replay.dto.ReplayRouteSource;
import com.travelmemory.replay.service.RoadRouteResult;
import com.travelmemory.replay.service.RoadRouteService;
import com.travelmemory.trip.entity.StopCategory;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStatus;
import com.travelmemory.trip.entity.TripStop;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import com.travelmemory.user.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PublicTripServiceTest {

    private TripRepository tripRepository;
    private TripStopRepository stopRepository;
    private PhotoRepository photoRepository;
    private TripRatingRepository ratingRepository;
    private PhotoStorageService storageService;
    private RoadRouteService roadRouteService;
    private PublicTripService service;
    private UserProfile owner;

    @BeforeEach
    void setUp() {
        tripRepository = mock(TripRepository.class);
        stopRepository = mock(TripStopRepository.class);
        photoRepository = mock(PhotoRepository.class);
        ratingRepository = mock(TripRatingRepository.class);
        storageService = mock(PhotoStorageService.class);
        roadRouteService = mock(RoadRouteService.class);
        service = new PublicTripService(
                tripRepository, stopRepository, photoRepository, ratingRepository, storageService, roadRouteService);
        owner = new UserProfile(UUID.randomUUID(), "owner@example.com", "Owner Name");
    }

    @Test
    void buildsAnAnonymousSafeStoryFromOnlySelectedPhotos() {
        Trip trip = new Trip(
                owner, "Corfu in Blue", "Saltwater afternoons", "Greece", "GR", "Corfu",
                LocalDate.of(2025, 8, 17), LocalDate.of(2025, 8, 23), TripVisibility.PUBLIC);
        TripStop beach = new TripStop(
                trip, owner, "Paleokastritsa", "Clear blue water",
                new BigDecimal("39.6720"), new BigDecimal("19.7010"),
                OffsetDateTime.of(2025, 8, 18, 10, 0, 0, 0, ZoneOffset.UTC), null,
                StopCategory.BEACH, 10, 0);
        Photo selected = new Photo(
                UUID.randomUUID(), trip, beach, owner, trip.getId() + "/selected.jpg",
                "selected.jpg", "image/jpeg", 128, beach.getArrivalTime(), null, null, "Morning swim");
        selected.setPublicVisible(true);
        TripRating rating = new TripRating(trip, owner, 9, 7, 8, 10, 8, 9, 7, 10, WouldReturn.YES);
        when(tripRepository.findByPublicSlugAndVisibilityAndStatusNot(
                trip.getPublicSlug(), TripVisibility.PUBLIC, TripStatus.ARCHIVED)).thenReturn(Optional.of(trip));
        when(stopRepository.findByTripIdOrderByPositionAsc(trip.getId())).thenReturn(List.of(beach));
        when(photoRepository.findByTripIdAndPublicVisibleTrueOrderByTakenAtAscCreatedAtAsc(trip.getId()))
                .thenReturn(List.of(selected));
        when(ratingRepository.findByTripId(trip.getId())).thenReturn(List.of(rating));
        when(storageService.createPublicSignedUrl(selected.getStoragePath()))
                .thenReturn("https://signed.example/selected");
        when(roadRouteService.calculate(List.of(beach))).thenReturn(new RoadRouteResult(
                ReplayRouteSource.NO_ROUTE, "driving", new BigDecimal("0.0"), 0, List.of()));

        var response = service.getBySlug(trip.getPublicSlug());

        assertThat(response.title()).isEqualTo("Corfu in Blue");
        assertThat(response.statistics().travelDays()).isEqualTo(7);
        assertThat(response.statistics().placeCount()).isEqualTo(1);
        assertThat(response.statistics().selectedPhotoCount()).isEqualTo(1);
        assertThat(response.photos()).singleElement().satisfies(photo -> {
            assertThat(photo.signedUrl()).isEqualTo("https://signed.example/selected");
            assertThat(photo.caption()).isEqualTo("Morning swim");
        });
        assertThat(response.rating().averageScore()).isEqualByComparingTo("9.0");
        assertThat(response.rating().returnIntent().yes()).isEqualTo(1);
    }

    @Test
    void missingPrivateAndArchivedTripsAllLookNotFound() {
        when(tripRepository.findByPublicSlugAndVisibilityAndStatusNot(
                "hidden-trip", TripVisibility.PUBLIC, TripStatus.ARCHIVED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySlug("hidden-trip"))
                .isInstanceOf(PublicTripNotFoundException.class)
                .hasMessage("This public trip does not exist.");
        verifyNoInteractions(stopRepository, photoRepository, ratingRepository, storageService, roadRouteService);
    }
}
