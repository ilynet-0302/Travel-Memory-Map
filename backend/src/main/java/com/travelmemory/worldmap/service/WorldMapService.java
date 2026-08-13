package com.travelmemory.worldmap.service;

import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.expense.entity.Expense;
import com.travelmemory.expense.repository.ExpenseRepository;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import com.travelmemory.worldmap.dto.WorldMapCountryResponse;
import com.travelmemory.worldmap.dto.WorldMapCountryStatus;
import com.travelmemory.worldmap.dto.WorldMapResponse;
import com.travelmemory.worldmap.dto.WorldMapSpendingResponse;
import com.travelmemory.worldmap.dto.WorldMapTripResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorldMapService {

    private static final int WORLD_COUNTRY_COUNT = 195;

    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final PhotoRepository photoRepository;
    private final ExpenseRepository expenseRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final Clock clock;

    public WorldMapService(
            TripRepository tripRepository,
            TripStopRepository tripStopRepository,
            PhotoRepository photoRepository,
            ExpenseRepository expenseRepository,
            AuthenticatedUserProvider authenticatedUserProvider,
            Clock clock) {
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.photoRepository = photoRepository;
        this.expenseRepository = expenseRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public WorldMapResponse getCurrentUsersMap() {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        LocalDate today = LocalDate.now(clock);
        Map<String, List<Trip>> tripsByCountry = tripRepository.findAccessibleTrips(userId).stream()
                .collect(Collectors.groupingBy(Trip::getCountryCode, TreeMap::new, Collectors.toList()));
        List<WorldMapCountryResponse> countries = tripsByCountry.values().stream()
                .map(trips -> country(trips, today))
                .sorted(Comparator.comparing(WorldMapCountryResponse::status)
                        .thenComparing(WorldMapCountryResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int visited = (int) countries.stream()
                .filter(country -> country.status() == WorldMapCountryStatus.VISITED)
                .count();
        int planned = (int) countries.stream()
                .filter(country -> country.status() == WorldMapCountryStatus.PLANNED)
                .count();
        return new WorldMapResponse(visited, planned, WORLD_COUNTRY_COUNT, countries);
    }

    private WorldMapCountryResponse country(List<Trip> trips, LocalDate today) {
        List<Trip> orderedTrips = trips.stream()
                .sorted(Comparator.comparing(Trip::getStartDate).reversed())
                .toList();
        List<UUID> tripIds = orderedTrips.stream().map(Trip::getId).toList();
        long visitedTrips = orderedTrips.stream().filter(trip -> !trip.getStartDate().isAfter(today)).count();
        long plannedTrips = orderedTrips.size() - visitedTrips;
        Trip representative = orderedTrips.getFirst();
        return new WorldMapCountryResponse(
                representative.getCountryCode(),
                representative.getCountry(),
                visitedTrips > 0 ? WorldMapCountryStatus.VISITED : WorldMapCountryStatus.PLANNED,
                orderedTrips.size(),
                visitedTrips,
                plannedTrips,
                orderedTrips.stream().map(Trip::getCity).map(String::toLowerCase).distinct().count(),
                tripStopRepository.countByTripIdIn(tripIds),
                photoRepository.countByTripIdIn(tripIds),
                spending(tripIds),
                orderedTrips.stream().map(this::trip).toList());
    }

    private List<WorldMapSpendingResponse> spending(List<UUID> tripIds) {
        Map<String, BigDecimal> totals = expenseRepository.findByTripIdIn(tripIds).stream()
                .collect(Collectors.groupingBy(
                        Expense::getCurrency,
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
        return totals.entrySet().stream()
                .map(entry -> new WorldMapSpendingResponse(
                        entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    private WorldMapTripResponse trip(Trip trip) {
        return new WorldMapTripResponse(
                trip.getId(), trip.getTitle(), trip.getCity(), trip.getStartDate(), trip.getEndDate(), trip.getStatus());
    }
}
