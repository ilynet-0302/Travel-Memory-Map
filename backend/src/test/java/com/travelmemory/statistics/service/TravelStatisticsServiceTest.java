package com.travelmemory.statistics.service;

import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.entity.ExpenseCategory;
import com.travelmemory.expense.repository.ExpenseRepository;
import com.travelmemory.rating.entity.TripRating;
import com.travelmemory.rating.repository.TripRatingRepository;
import com.travelmemory.statistics.dto.TravelStatisticsResponse;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import com.travelmemory.user.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelStatisticsServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripStopRepository tripStopRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private TripRatingRepository tripRatingRepository;

    private TravelStatisticsService travelStatisticsService;

    @BeforeEach
    void setUp() {
        travelStatisticsService = new TravelStatisticsService(
                tripRepository,
                tripStopRepository,
                photoRepository,
                expenseRepository,
                tripRatingRepository,
                Clock.fixed(Instant.parse("2026-08-13T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void calculatesDeterministicStatisticsFromAccessibleTrips() {
        UUID userId = UUID.randomUUID();
        UserProfile owner = new UserProfile(userId, "traveller@example.com", "Traveller");
        Trip rome = new Trip(
                owner,
                "Rome",
                null,
                "Italy",
                "IT",
                "Rome",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 5),
                TripVisibility.PRIVATE);
        Trip florence = new Trip(
                owner,
                "Florence",
                null,
                "Italy",
                "IT",
                "Florence",
                LocalDate.of(2026, 8, 18),
                LocalDate.of(2026, 8, 20),
                TripVisibility.PRIVATE);

        when(tripRepository.findAccessibleTrips(userId)).thenReturn(List.of(rome, florence));
        when(tripStopRepository.countByTripIdIn(anyCollection())).thenReturn(7L);
        when(photoRepository.countByTripIdIn(anyCollection())).thenReturn(18L);
        when(expenseRepository.findByTripIdIn(anyCollection())).thenReturn(List.of());
        when(tripRatingRepository.findByTripIdIn(anyCollection())).thenReturn(List.of());

        TravelStatisticsResponse statistics = travelStatisticsService.calculateFor(userId);

        assertThat(statistics.countriesVisited()).isEqualTo(1);
        assertThat(statistics.citiesVisited()).isEqualTo(1);
        assertThat(statistics.trips()).isEqualTo(2);
        assertThat(statistics.completedTrips()).isEqualTo(1);
        assertThat(statistics.placesVisited()).isEqualTo(7);
        assertThat(statistics.photosUploaded()).isEqualTo(18);
        assertThat(statistics.travelDays()).isEqualTo(5);
        assertThat(statistics.spending()).isEmpty();
        assertThat(statistics.favouriteCountry()).isNull();
        assertThat(statistics.mostVisitedCountry().name()).isEqualTo("Italy");
        assertThat(statistics.longestTrip().title()).isEqualTo("Rome");
    }

    @Test
    void keepsCurrenciesSeparateAndUsesRatingsForFavouriteLocations() {
        UUID userId = UUID.randomUUID();
        UserProfile owner = new UserProfile(userId, "traveller@example.com", "Traveller");
        Trip rome = trip(owner, "Rome", "Italy", "IT", "Rome", 1, 5);
        Trip florence = trip(owner, "Florence", "Italy", "IT", "Florence", 6, 8);
        Trip budapest = trip(owner, "Budapest", "Hungary", "HU", "Budapest", 9, 10);
        List<Expense> expenses = List.of(
                expense(rome, owner, "Hotel", "300.00", "EUR"),
                expense(florence, owner, "Dinner", "100.00", "EUR"),
                expense(budapest, owner, "Hotel", "200.00", "USD"));
        List<TripRating> ratings = List.of(
                new TripRating(rome, owner, 9),
                new TripRating(florence, owner, 7),
                new TripRating(budapest, owner, 10));

        when(tripRepository.findAccessibleTrips(userId)).thenReturn(List.of(rome, florence, budapest));
        when(tripStopRepository.countByTripIdIn(anyCollection())).thenReturn(0L);
        when(photoRepository.countByTripIdIn(anyCollection())).thenReturn(0L);
        when(expenseRepository.findByTripIdIn(anyCollection())).thenReturn(expenses);
        when(tripRatingRepository.findByTripIdIn(anyCollection())).thenReturn(ratings);

        TravelStatisticsResponse statistics = travelStatisticsService.calculateFor(userId);

        assertThat(statistics.spending()).extracting("currency").containsExactly("EUR", "USD");
        assertThat(statistics.spending().getFirst().totalSpent()).isEqualByComparingTo("400.00");
        assertThat(statistics.spending().getFirst().averageCostPerTrip()).isEqualByComparingTo("200.00");
        assertThat(statistics.favouriteCountry().name()).isEqualTo("Hungary");
        assertThat(statistics.favouriteCity().name()).isEqualTo("Budapest");
        assertThat(statistics.mostVisitedCountry().name()).isEqualTo("Italy");
        assertThat(statistics.longestTrip().title()).isEqualTo("Rome");
        assertThat(statistics.shortestTrip().title()).isEqualTo("Budapest");
    }

    private Trip trip(
            UserProfile owner, String title, String country, String countryCode, String city, int startDay, int endDay) {
        return new Trip(
                owner, title, null, country, countryCode, city,
                LocalDate.of(2026, 8, startDay), LocalDate.of(2026, 8, endDay), TripVisibility.PRIVATE);
    }

    private Expense expense(Trip trip, UserProfile owner, String title, String amount, String currency) {
        return new Expense(
                trip, title, new BigDecimal(amount), currency, ExpenseCategory.ACCOMMODATION,
                trip.getStartDate(), owner, owner, List.of(owner));
    }
}
