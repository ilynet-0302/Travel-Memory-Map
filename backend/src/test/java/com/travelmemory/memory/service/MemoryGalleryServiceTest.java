package com.travelmemory.memory.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.memory.dto.MemoryPhotoResponse;
import com.travelmemory.photo.entity.Photo;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.photo.service.PhotoStorageService;
import com.travelmemory.trip.entity.StopCategory;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStop;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.user.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemoryGalleryServiceTest {

    private TripRepository tripRepository;
    private PhotoRepository photoRepository;
    private PhotoStorageService storageService;
    private MemoryGalleryService service;
    private UserProfile traveller;

    @BeforeEach
    void setUp() {
        tripRepository = mock(TripRepository.class);
        photoRepository = mock(PhotoRepository.class);
        storageService = mock(PhotoStorageService.class);
        AuthenticatedUserProvider userProvider = mock(AuthenticatedUserProvider.class);
        traveller = new UserProfile(UUID.randomUUID(), "traveller@example.com", "Traveller");
        when(userProvider.getCurrentUser()).thenReturn(new AuthenticatedUser(
                traveller.getId(), traveller.getEmail(), traveller.getDisplayName()));
        when(userProvider.getCurrentAccessToken()).thenReturn("access-token");
        service = new MemoryGalleryService(tripRepository, photoRepository, storageService, userProvider);
    }

    @Test
    void returnsSignedPhotosOnlyFromTripsAccessibleToTheCurrentUser() {
        Trip rome = new Trip(
                traveller, "Roman Holiday", null, "Italy", "IT", "Rome",
                LocalDate.parse("2025-09-12"), LocalDate.parse("2025-09-16"), TripVisibility.PRIVATE);
        TripStop colosseum = new TripStop(
                rome, traveller, "Colosseum", null, BigDecimal.ONE, BigDecimal.ONE,
                OffsetDateTime.parse("2025-09-13T10:00:00Z"), null, StopCategory.LANDMARK, 9, 0);
        Photo photo = new Photo(
                UUID.randomUUID(), rome, colosseum, traveller, rome.getId() + "/rome.jpg",
                "rome.jpg", "image/jpeg", 2048, OffsetDateTime.parse("2025-09-13T10:05:00Z"),
                new BigDecimal("41.8902"), new BigDecimal("12.4922"), "Golden morning");
        when(tripRepository.findAccessibleTrips(traveller.getId())).thenReturn(List.of(rome));
        when(photoRepository.findByTripIdIn(List.of(rome.getId()))).thenReturn(List.of(photo));
        when(storageService.createSignedUrl(photo.getStoragePath(), "access-token"))
                .thenReturn("https://signed.example/rome");

        List<MemoryPhotoResponse> memories = service.getCurrentUsersMemories();

        assertThat(memories).singleElement().satisfies(memory -> {
            assertThat(memory.tripId()).isEqualTo(rome.getId());
            assertThat(memory.tripTitle()).isEqualTo("Roman Holiday");
            assertThat(memory.tripStopName()).isEqualTo("Colosseum");
            assertThat(memory.signedUrl()).isEqualTo("https://signed.example/rome");
            assertThat(memory.caption()).isEqualTo("Golden morning");
        });
        verify(photoRepository).findByTripIdIn(List.of(rome.getId()));
    }

    @Test
    void skipsPhotoLookupWhenTheUserHasNoAccessibleTrips() {
        when(tripRepository.findAccessibleTrips(traveller.getId())).thenReturn(List.of());

        assertThat(service.getCurrentUsersMemories()).isEmpty();
    }
}
