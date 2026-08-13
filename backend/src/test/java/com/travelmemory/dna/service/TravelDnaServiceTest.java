package com.travelmemory.dna.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.dna.dto.TravelPersonalityResponse;
import com.travelmemory.dna.dto.TripDnaResponse;
import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.entity.ExpenseCategory;
import com.travelmemory.expense.repository.ExpenseRepository;
import com.travelmemory.membership.service.TripPermissionService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TravelDnaServiceTest {

    private TripRepository tripRepository;
    private TripStopRepository stopRepository;
    private ExpenseRepository expenseRepository;
    private TripPermissionService permissionService;
    private AuthenticatedUserProvider userProvider;
    private TravelDnaService service;
    private UserProfile owner;
    private Trip trip;

    @BeforeEach
    void setUp() {
        tripRepository = mock(TripRepository.class);
        stopRepository = mock(TripStopRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        permissionService = mock(TripPermissionService.class);
        userProvider = mock(AuthenticatedUserProvider.class);
        service = new TravelDnaService(
                tripRepository, stopRepository, expenseRepository,
                permissionService, userProvider,
                Clock.fixed(Instant.parse("2026-08-13T10:00:00Z"), ZoneOffset.UTC));
        owner = new UserProfile(UUID.randomUUID(), "owner@example.com", "Owner");
        trip = trip("Romanian road trip", 1, 5);
        when(userProvider.getCurrentUser()).thenReturn(new AuthenticatedUser(
                owner.getId(), owner.getEmail(), owner.getDisplayName()));
        when(tripRepository.findById(trip.getId())).thenReturn(java.util.Optional.of(trip));
    }

    @Test
    void calculatesExplainableTripDnaFromStopsPaceAndExpenses() {
        when(stopRepository.findByTripIdOrderByPositionAsc(trip.getId())).thenReturn(List.of(
                stop(trip, StopCategory.RESTAURANT, 12, 0),
                stop(trip, StopCategory.MUSEUM, 15, 1),
                stop(trip, StopCategory.NATURE, 10, 2),
                stop(trip, StopCategory.BAR, 22, 3),
                stop(trip, StopCategory.LANDMARK, 9, 4)));
        when(expenseRepository.findByTripId(trip.getId())).thenReturn(List.of(
                expense(trip, "Dinner", "100", ExpenseCategory.FOOD),
                expense(trip, "Museum", "80", ExpenseCategory.ACTIVITY)));

        TripDnaResponse response = service.forTrip(trip.getId());

        assertThat(response.dominantTrait()).isEqualTo("EXPLORER");
        assertThat(response.scores().explorer()).isEqualTo(67);
        assertThat(response.scores().foodie()).isEqualTo(51);
        assertThat(response.scores().culture()).isEqualTo(47);
        assertThat(response.signals()).hasSize(3).anyMatch(signal -> signal.contains("5 saved places"));
        verify(permissionService).requireViewAccess(trip, owner.getId());
    }

    @Test
    void revealsPersonalityOnlyAfterThreeCompletedTrips() {
        Trip second = trip("Second", 6, 7);
        Trip third = trip("Third", 8, 9);
        when(tripRepository.findAccessibleTrips(owner.getId())).thenReturn(List.of(trip, second, third));
        for (Trip completedTrip : List.of(trip, second, third)) {
            when(stopRepository.findByTripIdOrderByPositionAsc(completedTrip.getId())).thenReturn(List.of(
                    stop(completedTrip, StopCategory.LANDMARK, 10, 0),
                    stop(completedTrip, StopCategory.MUSEUM, 14, 1),
                    stop(completedTrip, StopCategory.RESTAURANT, 18, 2)));
            when(expenseRepository.findByTripId(completedTrip.getId())).thenReturn(List.of());
        }

        TravelPersonalityResponse response = service.personalityFor(owner.getId());

        assertThat(response.revealed()).isTrue();
        assertThat(response.completedTrips()).isEqualTo(3);
        assertThat(response.type()).isEqualTo("Culture Seeker");
    }

    private Trip trip(String title, int startDay, int endDay) {
        return new Trip(
                owner, title, null, "Romania", "RO", "Timisoara",
                LocalDate.of(2026, 8, startDay), LocalDate.of(2026, 8, endDay), TripVisibility.PRIVATE);
    }

    private TripStop stop(Trip value, StopCategory category, int hour, int position) {
        OffsetDateTime arrival = OffsetDateTime.of(
                value.getStartDate(), java.time.LocalTime.of(hour, 0), ZoneOffset.UTC);
        return new TripStop(
                value, owner, category.name(), null, new BigDecimal("45.750000"),
                new BigDecimal("21.230000"), arrival, null, category, null, position);
    }

    private Expense expense(Trip value, String title, String amount, ExpenseCategory category) {
        return new Expense(
                value, title, new BigDecimal(amount), "EUR", category, value.getStartDate(),
                owner, owner, List.of(owner));
    }
}
