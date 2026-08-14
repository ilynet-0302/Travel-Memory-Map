package com.travelmemory.trip.service;

import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.dna.dto.TripDnaResponse;
import com.travelmemory.dna.service.TravelDnaService;
import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.repository.ExpenseRepository;
import com.travelmemory.membership.entity.TripMember;
import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.membership.repository.TripMemberRepository;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.rating.entity.TripRating;
import com.travelmemory.rating.repository.TripRatingRepository;
import com.travelmemory.trip.dto.TripRelationship;
import com.travelmemory.trip.dto.TripSearchCriteria;
import com.travelmemory.trip.dto.TripSearchResultResponse;
import com.travelmemory.trip.dto.TripSearchSort;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStop;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TripSearchService {

    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final ExpenseRepository expenseRepository;
    private final TripRatingRepository tripRatingRepository;
    private final TripMemberRepository tripMemberRepository;
    private final PhotoRepository photoRepository;
    private final TravelDnaService travelDnaService;

    public TripSearchService(
            AuthenticatedUserProvider authenticatedUserProvider,
            TripRepository tripRepository,
            TripStopRepository tripStopRepository,
            ExpenseRepository expenseRepository,
            TripRatingRepository tripRatingRepository,
            TripMemberRepository tripMemberRepository,
            PhotoRepository photoRepository,
            TravelDnaService travelDnaService) {
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.expenseRepository = expenseRepository;
        this.tripRatingRepository = tripRatingRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.photoRepository = photoRepository;
        this.travelDnaService = travelDnaService;
    }

    @Transactional(readOnly = true)
    public List<TripSearchResultResponse> search(TripSearchCriteria criteria) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        List<Trip> accessibleTrips = tripRepository.findAccessibleTrips(userId);
        if (accessibleTrips.isEmpty()) return List.of();

        List<UUID> tripIds = accessibleTrips.stream().map(Trip::getId).toList();
        Map<UUID, List<TripStop>> stops = groupByTrip(tripStopRepository.findByTripIdIn(tripIds), TripStop::getTrip);
        Map<UUID, List<Expense>> expenses = groupByTrip(expenseRepository.findByTripIdIn(tripIds), Expense::getTrip);
        Map<UUID, List<TripRating>> ratings = groupByTrip(tripRatingRepository.findByTripIdIn(tripIds), TripRating::getTrip);

        List<SearchableTrip> searchableTrips = new ArrayList<>();
        for (Trip trip : accessibleTrips) {
            List<TripStop> tripStops = stops.getOrDefault(trip.getId(), List.of());
            List<Expense> tripExpenses = expenses.getOrDefault(trip.getId(), List.of());
            List<TripRating> tripRatings = ratings.getOrDefault(trip.getId(), List.of());
            TripDnaResponse dna = travelDnaService.calculate(trip, tripStops, tripExpenses);
            searchableTrips.add(new SearchableTrip(
                    trip,
                    tripStops,
                    averageRating(tripRatings),
                    dna.dominantTrait(),
                    totalInCurrency(tripExpenses, criteria.currency()),
                    duration(trip)));
        }

        return searchableTrips.stream()
                .filter(trip -> matches(trip, criteria, userId))
                .sorted(comparator(criteria.sort()))
                .map(trip -> response(trip, userId, criteria.currency()))
                .toList();
    }

    private boolean matches(SearchableTrip item, TripSearchCriteria criteria, UUID userId) {
        Trip trip = item.trip();
        return matchesQuery(item, criteria.q())
                && (criteria.status() == null || trip.getStatus() == criteria.status())
                && (criteria.year() == null || overlapsYear(trip, criteria.year()))
                && contains(trip.getCountry(), criteria.country())
                && contains(trip.getCity(), criteria.city())
                && (criteria.minRating() == null
                    || item.averageRating() != null && item.averageRating().compareTo(criteria.minRating()) >= 0)
                && (criteria.minPrice() == null || item.totalSpent().compareTo(criteria.minPrice()) >= 0)
                && (criteria.maxPrice() == null || item.totalSpent().compareTo(criteria.maxPrice()) <= 0)
                && (criteria.minDuration() == null || item.durationDays() >= criteria.minDuration())
                && (criteria.maxDuration() == null || item.durationDays() <= criteria.maxDuration())
                && (criteria.dna() == null || item.dominantTrait().equalsIgnoreCase(criteria.dna()))
                && matchesRelationship(trip, criteria.relationship(), userId);
    }

    private boolean matchesQuery(SearchableTrip item, String query) {
        if (query == null) return true;
        String needle = query.toLowerCase(Locale.ROOT);
        Trip trip = item.trip();
        return List.of(
                        trip.getTitle(), trip.getCountry(), trip.getCity(),
                        String.valueOf(trip.getStartDate().getYear()), String.valueOf(trip.getEndDate().getYear()))
                .stream().anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(needle))
                || item.stops().stream().anyMatch(stop -> stop.getName().toLowerCase(Locale.ROOT).contains(needle));
    }

    private boolean matchesRelationship(Trip trip, TripRelationship relationship, UUID userId) {
        boolean owner = trip.getOwner().getId().equals(userId);
        return relationship == TripRelationship.ALL
                || relationship == TripRelationship.OWNER && owner
                || relationship == TripRelationship.SHARED && !owner;
    }

    private boolean overlapsYear(Trip trip, int year) {
        return trip.getStartDate().getYear() <= year && trip.getEndDate().getYear() >= year;
    }

    private boolean contains(String value, String filter) {
        return filter == null || value.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }

    private Comparator<SearchableTrip> comparator(TripSearchSort sort) {
        Comparator<SearchableTrip> byStartDesc = Comparator.comparing(
                (SearchableTrip item) -> item.trip().getStartDate()).reversed();
        return switch (sort) {
            case START_ASC -> Comparator.comparing(item -> item.trip().getStartDate());
            case TITLE_ASC -> Comparator.comparing(item -> item.trip().getTitle(), String.CASE_INSENSITIVE_ORDER);
            case RATING_DESC -> Comparator.comparing(
                    SearchableTrip::averageRating,
                    Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(byStartDesc);
            case PRICE_DESC -> Comparator.comparing(SearchableTrip::totalSpent).reversed().thenComparing(byStartDesc);
            case DURATION_DESC -> Comparator.comparingInt(SearchableTrip::durationDays).reversed().thenComparing(byStartDesc);
            case START_DESC -> byStartDesc;
        };
    }

    private TripSearchResultResponse response(SearchableTrip item, UUID userId, String currency) {
        Trip trip = item.trip();
        TripRole role = tripMemberRepository.findByTripIdAndUserId(trip.getId(), userId)
                .map(TripMember::getRole)
                .orElse(null);
        return new TripSearchResultResponse(
                trip.getId(), trip.getTitle(), trip.getDescription(), trip.getCountry(), trip.getCountryCode(),
                trip.getCity(), trip.getStartDate(), trip.getEndDate(), trip.getStatus(), trip.getVisibility(),
                trip.getCoverImageUrl(), role, tripMemberRepository.countByTripId(trip.getId()), item.stops().size(),
                photoRepository.countByTripId(trip.getId()), item.durationDays(), item.averageRating(),
                item.dominantTrait(), item.totalSpent(), currency);
    }

    private BigDecimal averageRating(List<TripRating> ratings) {
        if (ratings.isEmpty()) return null;
        return BigDecimal.valueOf(ratings.stream().mapToInt(TripRating::getOverallScore).average().orElseThrow())
                .setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal totalInCurrency(List<Expense> expenses, String currency) {
        return expenses.stream()
                .filter(expense -> expense.getCurrency().equalsIgnoreCase(currency))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int duration(Trip trip) {
        return Math.toIntExact(Math.max(1, ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1));
    }

    private <T> Map<UUID, List<T>> groupByTrip(Collection<T> values, Function<T, Trip> tripExtractor) {
        return values.stream().collect(Collectors.groupingBy(value -> tripExtractor.apply(value).getId()));
    }

    private record SearchableTrip(
            Trip trip,
            List<TripStop> stops,
            BigDecimal averageRating,
            String dominantTrait,
            BigDecimal totalSpent,
            int durationDays) {
    }
}
