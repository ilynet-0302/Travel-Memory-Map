package com.travelmemory.memory.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.entity.ExpenseCategory;
import com.travelmemory.expense.repository.ExpenseRepository;
import com.travelmemory.memory.dto.OnThisDayResponse;
import com.travelmemory.photo.entity.Photo;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.photo.service.PhotoStorageService;
import com.travelmemory.trip.entity.StopCategory;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStop;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import com.travelmemory.user.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OnThisDayServiceTest {

    private TripRepository tripRepository;
    private TripStopRepository stopRepository;
    private PhotoRepository photoRepository;
    private ExpenseRepository expenseRepository;
    private PhotoStorageService storageService;
    private OnThisDayService service;
    private UserProfile owner;

    @BeforeEach
    void setUp() {
        tripRepository = mock(TripRepository.class);
        stopRepository = mock(TripStopRepository.class);
        photoRepository = mock(PhotoRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        storageService = mock(PhotoStorageService.class);
        AuthenticatedUserProvider userProvider = mock(AuthenticatedUserProvider.class);
        owner = new UserProfile(UUID.randomUUID(), "owner@example.com", "Owner");
        when(userProvider.getCurrentUser()).thenReturn(new AuthenticatedUser(
                owner.getId(), owner.getEmail(), owner.getDisplayName()));
        when(userProvider.getCurrentAccessToken()).thenReturn("access-token");
        service = new OnThisDayService(
                tripRepository, stopRepository, photoRepository, expenseRepository,
                storageService, userProvider,
                Clock.fixed(Instant.parse("2026-08-13T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void buildsCalendarMemoriesFromStopsPhotosAndExpenses() {
        Trip budapest = trip("Budapest After Dark", "Hungary", "HU", "Budapest", "2023-08-12", "2023-08-15");
        Trip rome = trip("Roman Holiday", "Italy", "IT", "Rome", "2025-08-13", "2025-08-14");
        Trip noMatch = trip("May in Paris", "France", "FR", "Paris", "2024-05-03", "2024-05-05");
        TripStop overnightSpa = stop(budapest, "Rudas Baths", "2023-08-12T22:00:00Z", "2023-08-13T01:00:00Z", 0);
        TripStop gallery = stop(budapest, "Hungarian National Gallery", "2023-08-13T11:00:00Z", null, 1);
        TripStop nextDay = stop(budapest, "Central Market", "2023-08-14T10:00:00Z", null, 2);
        Photo galleryPhoto = photo(budapest, gallery, "gallery.jpg", "2023-08-13T11:15:00Z", "Blue hour");
        Photo stopPhotoWithoutExif = photo(budapest, overnightSpa, "spa.jpg", null, null);
        Photo nextDayPhoto = photo(budapest, nextDay, "market.jpg", "2023-08-14T10:30:00Z", null);
        when(tripRepository.findAccessibleTrips(owner.getId())).thenReturn(List.of(noMatch, budapest, rome));
        when(stopRepository.findByTripIdOrderByPositionAsc(budapest.getId()))
                .thenReturn(List.of(overnightSpa, gallery, nextDay));
        when(stopRepository.findByTripIdOrderByPositionAsc(rome.getId())).thenReturn(List.of());
        when(photoRepository.findByTripIdOrderByTakenAtAscCreatedAtAsc(budapest.getId()))
                .thenReturn(List.of(galleryPhoto, stopPhotoWithoutExif, nextDayPhoto));
        when(photoRepository.findByTripIdOrderByTakenAtAscCreatedAtAsc(rome.getId())).thenReturn(List.of());
        when(expenseRepository.findByTripId(budapest.getId())).thenReturn(List.of(
                expense(budapest, "Dinner", "62.40", "EUR", "2023-08-13"),
                expense(budapest, "Museum", "19.60", "EUR", "2023-08-13"),
                expense(budapest, "Taxi", "24.00", "BGN", "2023-08-13"),
                expense(budapest, "Breakfast", "12.00", "EUR", "2023-08-14")));
        when(expenseRepository.findByTripId(rome.getId())).thenReturn(List.of());
        when(storageService.createSignedUrl(galleryPhoto.getStoragePath(), "access-token"))
                .thenReturn("https://signed.example/gallery");

        OnThisDayResponse response = service.getCurrentUsersMemories();

        assertThat(response.date()).isEqualTo("2026-08-13");
        assertThat(response.memories()).extracting("tripTitle")
                .containsExactly("Roman Holiday", "Budapest After Dark");
        var memory = response.memories().get(1);
        assertThat(memory.memoryDate()).isEqualTo("2023-08-13");
        assertThat(memory.yearsAgo()).isEqualTo(3);
        assertThat(memory.placeCount()).isEqualTo(2);
        assertThat(memory.placeNames()).containsExactly("Rudas Baths", "Hungarian National Gallery");
        assertThat(memory.photoCount()).isEqualTo(2);
        assertThat(memory.heroPhotoUrl()).isEqualTo("https://signed.example/gallery");
        assertThat(memory.heroPhotoCaption()).isEqualTo("Blue hour");
        assertThat(memory.spending()).extracting("currency").containsExactly("BGN", "EUR");
        assertThat(memory.spending().get(1).totalSpent()).isEqualByComparingTo("82.00");
    }

    @Test
    void returnsAnEmptyListWhenNoPastTripMatchesToday() {
        Trip april = trip("Spring break", "Portugal", "PT", "Lisbon", "2024-04-01", "2024-04-05");
        when(tripRepository.findAccessibleTrips(owner.getId())).thenReturn(List.of(april));

        OnThisDayResponse response = service.getCurrentUsersMemories();

        assertThat(response.date()).isEqualTo("2026-08-13");
        assertThat(response.memories()).isEmpty();
    }

    private Trip trip(
            String title, String country, String countryCode, String city,
            String startDate, String endDate) {
        return new Trip(
                owner, title, null, country, countryCode, city,
                LocalDate.parse(startDate), LocalDate.parse(endDate), TripVisibility.PRIVATE);
    }

    private TripStop stop(Trip trip, String name, String arrival, String departure, int position) {
        return new TripStop(
                trip, owner, name, null, BigDecimal.ONE, BigDecimal.ONE,
                OffsetDateTime.parse(arrival), departure == null ? null : OffsetDateTime.parse(departure),
                StopCategory.LANDMARK, 9, position);
    }

    private Photo photo(Trip trip, TripStop stop, String fileName, String takenAt, String caption) {
        return new Photo(
                UUID.randomUUID(), trip, stop, owner, trip.getId() + "/" + fileName,
                fileName, "image/jpeg", 128,
                takenAt == null ? null : OffsetDateTime.parse(takenAt), null, null, caption);
    }

    private Expense expense(Trip trip, String title, String amount, String currency, String date) {
        return new Expense(
                trip, title, new BigDecimal(amount), currency, ExpenseCategory.OTHER,
                LocalDate.parse(date), owner, owner, List.of(owner));
    }
}
