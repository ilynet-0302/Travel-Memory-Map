package com.travelmemory.statistics.service;

import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.repository.ExpenseRepository;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.rating.entity.TripRating;
import com.travelmemory.rating.repository.TripRatingRepository;
import com.travelmemory.statistics.dto.LocationStatisticResponse;
import com.travelmemory.statistics.dto.SpendingTripResponse;
import com.travelmemory.statistics.dto.TravelSpendingResponse;
import com.travelmemory.statistics.dto.TravelStatisticsResponse;
import com.travelmemory.statistics.dto.TripStatisticResponse;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TravelStatisticsService {

    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final PhotoRepository photoRepository;
    private final ExpenseRepository expenseRepository;
    private final TripRatingRepository tripRatingRepository;
    private final Clock clock;

    public TravelStatisticsService(
            TripRepository tripRepository,
            TripStopRepository tripStopRepository,
            PhotoRepository photoRepository,
            ExpenseRepository expenseRepository,
            TripRatingRepository tripRatingRepository,
            Clock clock) {
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.photoRepository = photoRepository;
        this.expenseRepository = expenseRepository;
        this.tripRatingRepository = tripRatingRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TravelStatisticsResponse calculateFor(UUID userId) {
        List<Trip> trips = tripRepository.findAccessibleTrips(userId);
        LocalDate today = LocalDate.now(clock);
        List<Trip> startedTrips = trips.stream()
                .filter(trip -> !trip.getStartDate().isAfter(today))
                .toList();
        List<Trip> completedTrips = startedTrips.stream()
                .filter(trip -> trip.getEndDate().isBefore(today))
                .toList();
        List<UUID> startedTripIds = startedTrips.stream().map(Trip::getId).toList();
        long placesVisited = startedTripIds.isEmpty() ? 0 : tripStopRepository.countByTripIdIn(startedTripIds);
        long photosUploaded = startedTripIds.isEmpty() ? 0 : photoRepository.countByTripIdIn(startedTripIds);
        long travelDays = startedTrips.stream()
                .mapToLong(trip -> ChronoUnit.DAYS.between(
                        trip.getStartDate(),
                        trip.getEndDate().isBefore(today) ? trip.getEndDate() : today) + 1)
                .sum();
        List<Expense> expenses = startedTripIds.isEmpty()
                ? List.of()
                : expenseRepository.findByTripIdIn(startedTripIds);
        List<TripRating> ratings = startedTripIds.isEmpty()
                ? List.of()
                : tripRatingRepository.findByTripIdIn(startedTripIds);
        List<LocationStatisticResponse> countryStatistics = locationStatistics(
                startedTrips, ratings, Trip::getCountry);
        List<LocationStatisticResponse> cityStatistics = locationStatistics(
                startedTrips, ratings, Trip::getCity);

        return new TravelStatisticsResponse(
                (int) startedTrips.stream().map(Trip::getCountryCode).distinct().count(),
                (int) startedTrips.stream().map(trip -> trip.getCountryCode() + ":" + trip.getCity()).distinct().count(),
                trips.size(),
                completedTrips.size(),
                placesVisited,
                photosUploaded,
                travelDays,
                spending(startedTrips, expenses, today),
                favourite(countryStatistics).orElse(null),
                favourite(cityStatistics).orElse(null),
                mostVisited(countryStatistics).orElse(null),
                tripHighlight(completedTrips, true),
                tripHighlight(completedTrips, false));
    }

    private List<TravelSpendingResponse> spending(List<Trip> trips, List<Expense> expenses, LocalDate today) {
        Map<UUID, Trip> tripsById = trips.stream().collect(Collectors.toMap(Trip::getId, Function.identity()));
        Map<UUID, Long> tripDays = trips.stream().collect(Collectors.toMap(
                Trip::getId,
                trip -> travelDays(trip, today)));
        Map<String, List<Expense>> byCurrency = expenses.stream().collect(Collectors.groupingBy(
                Expense::getCurrency,
                TreeMap::new,
                Collectors.toList()));
        return byCurrency.entrySet().stream()
                .map(entry -> spendingForCurrency(entry.getKey(), entry.getValue(), tripsById, tripDays))
                .toList();
    }

    private TravelSpendingResponse spendingForCurrency(
            String currency,
            List<Expense> expenses,
            Map<UUID, Trip> tripsById,
            Map<UUID, Long> tripDays) {
        Map<UUID, BigDecimal> amountByTrip = expenses.stream().collect(Collectors.groupingBy(
                expense -> expense.getTrip().getId(),
                LinkedHashMap::new,
                Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
        BigDecimal total = amountByTrip.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        long days = amountByTrip.keySet().stream().mapToLong(tripDays::get).sum();
        Comparator<Map.Entry<UUID, BigDecimal>> amountComparator = Map.Entry.comparingByValue();
        Map.Entry<UUID, BigDecimal> mostExpensive = amountByTrip.entrySet().stream()
                .max(amountComparator.thenComparing(entry -> tripsById.get(entry.getKey()).getTitle()))
                .orElseThrow();
        Map.Entry<UUID, BigDecimal> cheapest = amountByTrip.entrySet().stream()
                .min(amountComparator.thenComparing(entry -> tripsById.get(entry.getKey()).getTitle()))
                .orElseThrow();
        return new TravelSpendingResponse(
                currency,
                total,
                total.divide(BigDecimal.valueOf(amountByTrip.size()), 2, RoundingMode.HALF_UP),
                total.divide(BigDecimal.valueOf(Math.max(1, days)), 2, RoundingMode.HALF_UP),
                spendingTrip(mostExpensive, tripsById),
                spendingTrip(cheapest, tripsById));
    }

    private SpendingTripResponse spendingTrip(
            Map.Entry<UUID, BigDecimal> amount,
            Map<UUID, Trip> tripsById) {
        Trip trip = tripsById.get(amount.getKey());
        return new SpendingTripResponse(trip.getId(), trip.getTitle(), amount.getValue().setScale(2));
    }

    private List<LocationStatisticResponse> locationStatistics(
            List<Trip> trips,
            List<TripRating> ratings,
            Function<Trip, String> location) {
        Map<String, List<Trip>> tripsByLocation = trips.stream().collect(Collectors.groupingBy(
                location,
                TreeMap::new,
                Collectors.toList()));
        return tripsByLocation.entrySet().stream().map(entry -> {
            List<UUID> tripIds = entry.getValue().stream().map(Trip::getId).toList();
            List<Integer> scores = ratings.stream()
                    .filter(rating -> tripIds.contains(rating.getTrip().getId()))
                    .map(TripRating::getScore)
                    .toList();
            BigDecimal averageRating = scores.isEmpty()
                    ? null
                    : BigDecimal.valueOf(scores.stream().mapToInt(Integer::intValue).average().orElseThrow())
                            .setScale(1, RoundingMode.HALF_UP);
            return new LocationStatisticResponse(entry.getKey(), entry.getValue().size(), averageRating);
        }).toList();
    }

    private Optional<LocationStatisticResponse> favourite(List<LocationStatisticResponse> statistics) {
        return statistics.stream()
                .filter(statistic -> statistic.averageRating() != null)
                .sorted(Comparator.comparing(LocationStatisticResponse::averageRating).reversed()
                        .thenComparing(LocationStatisticResponse::tripCount, Comparator.reverseOrder())
                        .thenComparing(LocationStatisticResponse::name))
                .findFirst();
    }

    private Optional<LocationStatisticResponse> mostVisited(List<LocationStatisticResponse> statistics) {
        return statistics.stream()
                .sorted(Comparator.comparing(LocationStatisticResponse::tripCount).reversed()
                        .thenComparing(LocationStatisticResponse::name))
                .findFirst();
    }

    private TripStatisticResponse tripHighlight(List<Trip> trips, boolean longest) {
        Comparator<Trip> comparator = Comparator.comparingLong((Trip trip) -> travelDays(trip))
                .thenComparing(Trip::getTitle, String.CASE_INSENSITIVE_ORDER);
        Optional<Trip> trip = longest ? trips.stream().max(comparator) : trips.stream().min(comparator);
        return trip.map(value -> new TripStatisticResponse(
                value.getId(),
                value.getTitle(),
                value.getCountry(),
                value.getCity(),
                value.getStartDate(),
                value.getEndDate(),
                travelDays(value))).orElse(null);
    }

    private long travelDays(Trip trip, LocalDate today) {
        LocalDate effectiveEnd = trip.getEndDate().isBefore(today) ? trip.getEndDate() : today;
        return ChronoUnit.DAYS.between(trip.getStartDate(), effectiveEnd) + 1;
    }

    private long travelDays(Trip trip) {
        return ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;
    }
}
