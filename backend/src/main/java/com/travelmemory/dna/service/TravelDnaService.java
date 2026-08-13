package com.travelmemory.dna.service;

import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.dna.dto.TravelDnaScoresResponse;
import com.travelmemory.dna.dto.TravelPersonalityResponse;
import com.travelmemory.dna.dto.TripDnaResponse;
import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.entity.ExpenseCategory;
import com.travelmemory.expense.repository.ExpenseRepository;
import com.travelmemory.exception.TripNotFoundException;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.trip.entity.StopCategory;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStop;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TravelDnaService {

    private static final int PERSONALITY_TRIP_THRESHOLD = 3;

    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final ExpenseRepository expenseRepository;
    private final TripPermissionService tripPermissionService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final Clock clock;

    public TravelDnaService(
            TripRepository tripRepository,
            TripStopRepository tripStopRepository,
            ExpenseRepository expenseRepository,
            TripPermissionService tripPermissionService,
            AuthenticatedUserProvider authenticatedUserProvider,
            Clock clock) {
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.expenseRepository = expenseRepository;
        this.tripPermissionService = tripPermissionService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TripDnaResponse forTrip(UUID tripId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new TripNotFoundException(tripId));
        tripPermissionService.requireViewAccess(trip, userId);
        return calculate(trip);
    }

    @Transactional(readOnly = true)
    public TravelPersonalityResponse personalityFor(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        List<Trip> completedTrips = tripRepository.findAccessibleTrips(userId).stream()
                .filter(trip -> trip.getEndDate().isBefore(today))
                .toList();
        List<TravelDnaScoresResponse> tripScores = completedTrips.stream()
                .map(this::calculate)
                .map(TripDnaResponse::scores)
                .toList();
        TravelDnaScoresResponse scores = average(tripScores);
        Trait dominant = dominant(scores);
        boolean revealed = completedTrips.size() >= PERSONALITY_TRIP_THRESHOLD;
        return new TravelPersonalityResponse(
                revealed ? dominant.personality : "Still taking shape",
                revealed ? dominant.description : "Complete more journeys to reveal your travel personality.",
                revealed,
                completedTrips.size(),
                PERSONALITY_TRIP_THRESHOLD,
                scores);
    }

    private TripDnaResponse calculate(Trip trip) {
        List<TripStop> stops = tripStopRepository.findByTripIdOrderByPositionAsc(trip.getId());
        List<Expense> expenses = expenseRepository.findByTripId(trip.getId());
        long days = Math.max(1, ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1);
        double totalStops = Math.max(1, stops.size());
        long distinctCategories = stops.stream()
                .map(TripStop::getCategory)
                .filter(category -> category != StopCategory.OTHER)
                .distinct()
                .count();
        double pace = stops.size() / (double) days;
        double foodStops = stopRatio(stops, StopCategory.RESTAURANT, StopCategory.BAR);
        double cultureStops = stopRatio(stops, StopCategory.MUSEUM, StopCategory.LANDMARK);
        double relaxationStops = stopRatio(stops, StopCategory.BEACH, StopCategory.HOTEL, StopCategory.NATURE);
        double natureStops = stopRatio(stops, StopCategory.NATURE, StopCategory.BEACH);
        double adventureStops = stopRatio(stops, StopCategory.NATURE, StopCategory.TRANSPORT, StopCategory.AIRPORT);
        double barStops = count(stops, StopCategory.BAR) / totalStops;
        double lateStops = stops.stream()
                .filter(stop -> stop.getArrivalTime().getHour() >= 20 || stop.getArrivalTime().getHour() < 5)
                .count() / totalStops;
        double foodSpend = expenseRatio(expenses, ExpenseCategory.FOOD);
        double cultureSpend = expenseRatio(expenses, ExpenseCategory.ACTIVITY);
        double accommodationSpend = expenseRatio(expenses, ExpenseCategory.ACCOMMODATION);
        double activitySpend = expenseRatio(expenses, ExpenseCategory.ACTIVITY, ExpenseCategory.TRANSPORT);

        TravelDnaScoresResponse scores = new TravelDnaScoresResponse(
                score(10 + Math.min(1, distinctCategories / 6d) * 35 + Math.min(1, stops.size() / 10d) * 35
                        + Math.min(1, days / 10d) * 20),
                score(10 + foodStops * 60 + foodSpend * 30),
                score(10 + cultureStops * 65 + cultureSpend * 25),
                score(5 + barStops * 60 + lateStops * 35),
                score(10 + relaxationStops * 55 + Math.max(0, 1 - Math.min(1, pace / 3d)) * 25
                        + accommodationSpend * 10),
                score(10 + natureStops * 75 + cultureSpend * 15),
                score(10 + adventureStops * 50 + Math.min(1, pace / 4d) * 20 + activitySpend * 20));
        Trait dominant = dominant(scores);
        return new TripDnaResponse(trip.getId(), scores, dominant.name(), signals(stops, expenses, days));
    }

    private List<String> signals(List<TripStop> stops, List<Expense> expenses, long days) {
        List<String> signals = new ArrayList<>();
        signals.add(stops.size() + " saved " + (stops.size() == 1 ? "place" : "places") + " across " + days
                + (days == 1 ? " day" : " days"));
        stops.stream()
                .collect(() -> new EnumMap<StopCategory, Long>(StopCategory.class),
                        (counts, stop) -> counts.merge(stop.getCategory(), 1L, Long::sum),
                        (left, right) -> right.forEach((key, value) -> left.merge(key, value, Long::sum)))
                .entrySet().stream()
                .max(Map.Entry.<StopCategory, Long>comparingByValue().thenComparing(entry -> entry.getKey().name()))
                .ifPresent(entry -> signals.add(entry.getKey().name().toLowerCase().replace('_', ' ')
                        + " is the strongest place signal"));
        expenses.stream()
                .collect(() -> new EnumMap<ExpenseCategory, Long>(ExpenseCategory.class),
                        (totals, expense) -> totals.merge(expense.getCategory(), 1L, Long::sum),
                        (left, right) -> right.forEach((key, value) -> left.merge(key, value, Long::sum)))
                .entrySet().stream()
                .max(Map.Entry.<ExpenseCategory, Long>comparingByValue()
                        .thenComparing(entry -> entry.getKey().name()))
                .ifPresent(entry -> signals.add(entry.getKey().name().toLowerCase().replace('_', ' ')
                        + " appears most often in expenses"));
        return List.copyOf(signals);
    }

    private TravelDnaScoresResponse average(List<TravelDnaScoresResponse> scores) {
        if (scores.isEmpty()) return new TravelDnaScoresResponse(0, 0, 0, 0, 0, 0, 0);
        return new TravelDnaScoresResponse(
                average(scores.stream().mapToInt(TravelDnaScoresResponse::explorer).toArray()),
                average(scores.stream().mapToInt(TravelDnaScoresResponse::foodie).toArray()),
                average(scores.stream().mapToInt(TravelDnaScoresResponse::culture).toArray()),
                average(scores.stream().mapToInt(TravelDnaScoresResponse::nightlife).toArray()),
                average(scores.stream().mapToInt(TravelDnaScoresResponse::relaxation).toArray()),
                average(scores.stream().mapToInt(TravelDnaScoresResponse::nature).toArray()),
                average(scores.stream().mapToInt(TravelDnaScoresResponse::adventure).toArray()));
    }

    private int average(int[] values) {
        return (int) Math.round(java.util.Arrays.stream(values).average().orElse(0));
    }

    private Trait dominant(TravelDnaScoresResponse scores) {
        Map<Trait, Integer> values = new LinkedHashMap<>();
        values.put(Trait.EXPLORER, scores.explorer());
        values.put(Trait.FOODIE, scores.foodie());
        values.put(Trait.CULTURE, scores.culture());
        values.put(Trait.NIGHTLIFE, scores.nightlife());
        values.put(Trait.RELAXATION, scores.relaxation());
        values.put(Trait.NATURE, scores.nature());
        values.put(Trait.ADVENTURE, scores.adventure());
        return values.entrySet().stream()
                .max(Map.Entry.<Trait, Integer>comparingByValue()
                        .thenComparing(entry -> -entry.getKey().ordinal()))
                .map(Map.Entry::getKey)
                .orElse(Trait.EXPLORER);
    }

    private double stopRatio(List<TripStop> stops, StopCategory... categories) {
        if (stops.isEmpty()) return 0;
        return java.util.Arrays.stream(categories).mapToLong(category -> count(stops, category)).sum()
                / (double) stops.size();
    }

    private long count(List<TripStop> stops, StopCategory category) {
        return stops.stream().filter(stop -> stop.getCategory() == category).count();
    }

    private double expenseRatio(List<Expense> expenses, ExpenseCategory... categories) {
        List<Double> currencyRatios = expenses.stream()
                .collect(java.util.stream.Collectors.groupingBy(Expense::getCurrency))
                .values().stream()
                .map(currencyExpenses -> {
                    BigDecimal total = currencyExpenses.stream()
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal selected = currencyExpenses.stream()
                            .filter(expense -> java.util.Arrays.asList(categories).contains(expense.getCategory()))
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return selected.divide(total, 6, java.math.RoundingMode.HALF_UP).doubleValue();
                })
                .toList();
        return currencyRatios.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private int score(double value) {
        return (int) Math.round(Math.max(0, Math.min(100, value)));
    }

    private enum Trait {
        EXPLORER("Urban Explorer", "You seek variety, movement and stories beyond a single landmark."),
        FOODIE("Food Hunter", "Local tables and flavours are the compass behind your journeys."),
        CULTURE("Culture Seeker", "Museums, landmarks and living history shape the way you travel."),
        NIGHTLIFE("Night Owl", "Your best travel stories tend to begin after sunset."),
        RELAXATION("Relaxed Traveler", "You leave room to slow down and let a destination unfold."),
        NATURE("Nature Wanderer", "Coasts, trails and open landscapes pull you off the usual route."),
        ADVENTURE("Adventure Traveler", "A full itinerary and active days are part of the thrill." );

        private final String personality;
        private final String description;

        Trait(String personality, String description) {
            this.personality = personality;
            this.description = description;
        }
    }
}
