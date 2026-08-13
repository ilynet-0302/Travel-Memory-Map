package com.travelmemory.comparison.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.comparison.dto.TripComparisonResponse;
import com.travelmemory.exception.InvalidTripComparisonException;
import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.entity.ExpenseCategory;
import com.travelmemory.expense.repository.ExpenseRepository;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.rating.entity.TripRating;
import com.travelmemory.rating.entity.WouldReturn;
import com.travelmemory.rating.repository.TripRatingRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import com.travelmemory.user.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TripComparisonServiceTest {

    private TripRepository tripRepository;
    private TripStopRepository stopRepository;
    private PhotoRepository photoRepository;
    private ExpenseRepository expenseRepository;
    private TripRatingRepository ratingRepository;
    private TripPermissionService permissionService;
    private TripComparisonService service;
    private UserProfile owner;
    private Trip rome;
    private Trip barcelona;

    @BeforeEach
    void setUp() {
        tripRepository = mock(TripRepository.class);
        stopRepository = mock(TripStopRepository.class);
        photoRepository = mock(PhotoRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        ratingRepository = mock(TripRatingRepository.class);
        permissionService = mock(TripPermissionService.class);
        AuthenticatedUserProvider userProvider = mock(AuthenticatedUserProvider.class);
        service = new TripComparisonService(
                tripRepository, stopRepository, photoRepository, expenseRepository,
                ratingRepository, permissionService, userProvider);
        owner = new UserProfile(UUID.randomUUID(), "owner@example.com", "Owner");
        rome = trip("Rome", "Italy", "IT", "Rome", 1, 5);
        barcelona = trip("Barcelona", "Spain", "ES", "Barcelona", 10, 13);
        when(userProvider.getCurrentUser()).thenReturn(new AuthenticatedUser(
                owner.getId(), owner.getEmail(), owner.getDisplayName()));
        when(tripRepository.findById(rome.getId())).thenReturn(Optional.of(rome));
        when(tripRepository.findById(barcelona.getId())).thenReturn(Optional.of(barcelona));
    }

    @Test
    void comparesRatingsFootprintAndSpendingWithoutMixingCurrencies() {
        TripRating romeOwnerRating = new TripRating(
                rome, owner, 9, 7, 10, 7, 9, 6, 7, 8, WouldReturn.YES);
        TripRating romeGuestRating = new TripRating(
                rome, new UserProfile(UUID.randomUUID(), "guest@example.com", "Guest"),
                8, 7, 8, 8, 9, 7, 6, 8, WouldReturn.MAYBE);
        when(ratingRepository.findByTripId(rome.getId()))
                .thenReturn(List.of(romeOwnerRating, romeGuestRating));
        when(ratingRepository.findByTripId(barcelona.getId())).thenReturn(List.of());
        when(stopRepository.countByTripId(rome.getId())).thenReturn(12L);
        when(photoRepository.countByTripId(rome.getId())).thenReturn(24L);
        when(expenseRepository.findByTripId(rome.getId())).thenReturn(List.of(
                expense(rome, "Dinner", "100.00", "EUR"),
                expense(rome, "Museum", "25.00", "EUR"),
                expense(rome, "Airport", "40.00", "USD")));
        when(expenseRepository.findByTripId(barcelona.getId())).thenReturn(List.of());

        TripComparisonResponse response = service.compare(rome.getId(), barcelona.getId());

        assertThat(response.left().title()).isEqualTo("Rome");
        assertThat(response.left().travelDays()).isEqualTo(5);
        assertThat(response.left().stopCount()).isEqualTo(12);
        assertThat(response.left().photoCount()).isEqualTo(24);
        assertThat(response.left().averageScore()).isEqualByComparingTo("8.0");
        assertThat(response.left().ratings().food()).isEqualByComparingTo("8.5");
        assertThat(response.left().wouldReturnYesPercent()).isEqualByComparingTo("50");
        assertThat(response.left().spending()).extracting("currency", "total", "costPerDay")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("EUR", new BigDecimal("125.00"), new BigDecimal("25.00")),
                        org.assertj.core.groups.Tuple.tuple("USD", new BigDecimal("40.00"), new BigDecimal("8.00")));
        assertThat(response.right().averageScore()).isNull();
        assertThat(response.right().spending()).isEmpty();
        verify(permissionService).requireViewAccess(rome, owner.getId());
        verify(permissionService).requireViewAccess(barcelona, owner.getId());
    }

    @Test
    void rejectsComparingATripWithItself() {
        assertThatThrownBy(() -> service.compare(rome.getId(), rome.getId()))
                .isInstanceOf(InvalidTripComparisonException.class)
                .hasMessage("Choose two different trips to compare.");
    }

    private Trip trip(String title, String country, String code, String city, int startDay, int endDay) {
        return new Trip(
                owner, title, null, country, code, city,
                LocalDate.of(2026, 7, startDay), LocalDate.of(2026, 7, endDay), TripVisibility.PRIVATE);
    }

    private Expense expense(Trip trip, String title, String amount, String currency) {
        return new Expense(
                trip, title, new BigDecimal(amount), currency, ExpenseCategory.OTHER,
                trip.getStartDate(), owner, owner, List.of(owner));
    }
}
