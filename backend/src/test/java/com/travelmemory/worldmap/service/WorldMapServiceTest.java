package com.travelmemory.worldmap.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.entity.ExpenseCategory;
import com.travelmemory.expense.repository.ExpenseRepository;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.worldmap.dto.WorldMapResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorldMapServiceTest {

    private TripRepository tripRepository;
    private TripStopRepository stopRepository;
    private PhotoRepository photoRepository;
    private ExpenseRepository expenseRepository;
    private WorldMapService service;
    private UserProfile owner;

    @BeforeEach
    void setUp() {
        tripRepository = mock(TripRepository.class);
        stopRepository = mock(TripStopRepository.class);
        photoRepository = mock(PhotoRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        AuthenticatedUserProvider userProvider = mock(AuthenticatedUserProvider.class);
        owner = new UserProfile(UUID.randomUUID(), "owner@example.com", "Owner");
        when(userProvider.getCurrentUser()).thenReturn(new AuthenticatedUser(
                owner.getId(), owner.getEmail(), owner.getDisplayName()));
        service = new WorldMapService(
                tripRepository, stopRepository, photoRepository, expenseRepository, userProvider,
                Clock.fixed(Instant.parse("2026-08-13T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void groupsVisitedAndPlannedTripsByCountryWithCountryTotals() {
        Trip rome = trip("Roman Holiday", "Italy", "IT", "Rome", "2026-05-01", "2026-05-05");
        Trip florence = trip("Florence", "Italy", "IT", "Florence", "2026-09-01", "2026-09-03");
        Trip japan = trip("Japan next", "Japan", "JP", "Tokyo", "2027-03-10", "2027-03-18");
        when(tripRepository.findAccessibleTrips(owner.getId())).thenReturn(List.of(japan, florence, rome));
        when(stopRepository.countByTripIdIn(argThat(ids -> ids != null && ids.contains(rome.getId()) && ids.contains(florence.getId()))))
                .thenReturn(11L);
        when(stopRepository.countByTripIdIn(argThat(ids -> ids != null && ids.size() == 1 && ids.contains(japan.getId()))))
                .thenReturn(4L);
        when(photoRepository.countByTripIdIn(argThat(ids -> ids != null && ids.contains(rome.getId()) && ids.contains(florence.getId()))))
                .thenReturn(52L);
        when(photoRepository.countByTripIdIn(argThat(ids -> ids != null && ids.size() == 1 && ids.contains(japan.getId()))))
                .thenReturn(0L);
        when(expenseRepository.findByTripIdIn(argThat(ids -> ids != null && ids.contains(rome.getId()) && ids.contains(florence.getId()))))
                .thenReturn(List.of(
                        expense(rome, "Dinner", "85.50", "EUR"),
                        expense(florence, "Hotel", "220.00", "EUR")));
        when(expenseRepository.findByTripIdIn(argThat(ids -> ids != null && ids.size() == 1 && ids.contains(japan.getId()))))
                .thenReturn(List.of());

        WorldMapResponse response = service.getCurrentUsersMap();

        assertThat(response.countriesVisited()).isEqualTo(1);
        assertThat(response.countriesPlanned()).isEqualTo(1);
        assertThat(response.countriesTotal()).isEqualTo(195);
        assertThat(response.countries()).extracting("countryCode").containsExactly("IT", "JP");
        var italy = response.countries().stream()
                .filter(country -> country.countryCode().equals("IT"))
                .findFirst().orElseThrow();
        assertThat(italy.status().name()).isEqualTo("VISITED");
        assertThat(italy.tripCount()).isEqualTo(2);
        assertThat(italy.visitedTripCount()).isEqualTo(1);
        assertThat(italy.plannedTripCount()).isEqualTo(1);
        assertThat(italy.cityCount()).isEqualTo(2);
        assertThat(italy.placeCount()).isEqualTo(11);
        assertThat(italy.photoCount()).isEqualTo(52);
        assertThat(italy.spending()).singleElement().satisfies(spending -> {
            assertThat(spending.currency()).isEqualTo("EUR");
            assertThat(spending.totalSpent()).isEqualByComparingTo("305.50");
        });
        assertThat(italy.trips()).extracting("title").containsExactly("Florence", "Roman Holiday");
    }

    private Trip trip(
            String title, String country, String countryCode, String city,
            String startDate, String endDate) {
        return new Trip(
                owner, title, null, country, countryCode, city,
                LocalDate.parse(startDate), LocalDate.parse(endDate), TripVisibility.PRIVATE);
    }

    private Expense expense(Trip trip, String title, String amount, String currency) {
        return new Expense(
                trip, title, new BigDecimal(amount), currency, ExpenseCategory.OTHER,
                trip.getStartDate(), owner, owner, List.of(owner));
    }
}
