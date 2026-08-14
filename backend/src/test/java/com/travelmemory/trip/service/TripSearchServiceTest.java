package com.travelmemory.trip.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.dna.dto.TravelDnaScoresResponse;
import com.travelmemory.dna.dto.TripDnaResponse;
import com.travelmemory.dna.service.TravelDnaService;
import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.entity.ExpenseCategory;
import com.travelmemory.expense.repository.ExpenseRepository;
import com.travelmemory.membership.entity.TripMember;
import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.membership.repository.TripMemberRepository;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.rating.entity.TripRating;
import com.travelmemory.rating.repository.TripRatingRepository;
import com.travelmemory.trip.dto.TripRelationship;
import com.travelmemory.trip.dto.TripSearchCriteria;
import com.travelmemory.trip.dto.TripSearchSort;
import com.travelmemory.trip.entity.StopCategory;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStop;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import com.travelmemory.user.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripSearchServiceTest {

    @Mock private AuthenticatedUserProvider authenticatedUserProvider;
    @Mock private TripRepository tripRepository;
    @Mock private TripStopRepository tripStopRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private TripRatingRepository tripRatingRepository;
    @Mock private TripMemberRepository tripMemberRepository;
    @Mock private PhotoRepository photoRepository;
    @Mock private TravelDnaService travelDnaService;

    private TripSearchService service;
    private UserProfile currentUser;

    @BeforeEach
    void setUp() {
        service = new TripSearchService(
                authenticatedUserProvider, tripRepository, tripStopRepository, expenseRepository,
                tripRatingRepository, tripMemberRepository, photoRepository, travelDnaService);
        currentUser = new UserProfile(UUID.randomUUID(), "traveller@example.com", "Traveller");
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(new AuthenticatedUser(
                currentUser.getId(), currentUser.getEmail(), currentUser.getDisplayName()));
    }

    @Test
    void searchesPlaceNamesInsideOnlyTheUsersAccessibleTrips() {
        Trip accessible = trip(currentUser, "Romania", "Timisoara", 5);
        TripStop gallery = new TripStop(
                accessible, currentUser, "Jecza Gallery", null,
                new BigDecimal("45.7304"), new BigDecimal("21.2382"),
                OffsetDateTime.of(2026, 8, 18, 11, 0, 0, 0, ZoneOffset.UTC), null,
                StopCategory.MUSEUM, 9, 0);
        stubAggregates(accessible, List.of(gallery), List.of(), List.of(), "CULTURE");

        List<?> results = service.search(criteria("jecza", null, null, null, null,
                null, null, null, null, null, null, TripRelationship.ALL, TripSearchSort.START_DESC, "EUR"));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst()).extracting("id").isEqualTo(accessible.getId());
    }

    @Test
    void appliesRatingPriceDurationDnaAndRelationshipFiltersTogether() {
        Trip accessible = trip(currentUser, "Italy", "Rome", 3);
        Expense expense = new Expense(
                accessible, "Hotel", new BigDecimal("120.00"), "EUR", ExpenseCategory.ACCOMMODATION,
                accessible.getStartDate(), currentUser, currentUser, List.of(currentUser));
        TripRating rating = new TripRating(accessible, currentUser, 8);
        stubAggregates(accessible, List.of(), List.of(expense), List.of(rating), "CULTURE");

        var matching = criteria(null, null, 2025, "ita", "rom", new BigDecimal("7"),
                new BigDecimal("100"), new BigDecimal("150"), 2, 4, "CULTURE",
                TripRelationship.OWNER, TripSearchSort.RATING_DESC, "EUR");
        var sharedOnly = criteria(null, null, null, null, null, null,
                null, null, null, null, null, TripRelationship.SHARED, TripSearchSort.START_DESC, "EUR");

        assertThat(service.search(matching)).singleElement().satisfies(result -> {
            assertThat(result.averageRating()).isEqualByComparingTo("8.0");
            assertThat(result.totalSpent()).isEqualByComparingTo("120.00");
            assertThat(result.durationDays()).isEqualTo(3);
            assertThat(result.dominantTrait()).isEqualTo("CULTURE");
        });
        assertThat(service.search(sharedOnly)).isEmpty();
    }

    private Trip trip(UserProfile owner, String country, String city, int durationDays) {
        LocalDate start = LocalDate.of(2025, 6, 10);
        return new Trip(
                owner, city + " story", null, country, country.substring(0, 2).toUpperCase(), city,
                start, start.plusDays(durationDays - 1L), TripVisibility.PRIVATE);
    }

    private void stubAggregates(
            Trip trip,
            List<TripStop> stops,
            List<Expense> expenses,
            List<TripRating> ratings,
            String dna) {
        when(tripRepository.findAccessibleTrips(currentUser.getId())).thenReturn(List.of(trip));
        when(tripStopRepository.findByTripIdIn(anyList())).thenReturn(stops);
        when(expenseRepository.findByTripIdIn(anyList())).thenReturn(expenses);
        when(tripRatingRepository.findByTripIdIn(anyList())).thenReturn(ratings);
        when(travelDnaService.calculate(trip, stops, expenses)).thenReturn(new TripDnaResponse(
                trip.getId(), new TravelDnaScoresResponse(20, 20, 80, 20, 20, 20, 20), dna, List.of()));
        when(tripMemberRepository.findByTripIdAndUserId(trip.getId(), currentUser.getId()))
                .thenReturn(Optional.of(new TripMember(trip, currentUser, TripRole.OWNER)));
        when(tripMemberRepository.countByTripId(trip.getId())).thenReturn(1L);
        when(photoRepository.countByTripId(trip.getId())).thenReturn(0L);
    }

    private TripSearchCriteria criteria(
            String q,
            com.travelmemory.trip.entity.TripStatus status,
            Integer year,
            String country,
            String city,
            BigDecimal minRating,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minDuration,
            Integer maxDuration,
            String dna,
            TripRelationship relationship,
            TripSearchSort sort,
            String currency) {
        return new TripSearchCriteria(
                q, status, year, country, city, minRating, minPrice, maxPrice,
                minDuration, maxDuration, dna, relationship, sort, currency);
    }
}
