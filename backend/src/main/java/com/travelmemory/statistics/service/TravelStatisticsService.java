package com.travelmemory.statistics.service;

import com.travelmemory.statistics.dto.TravelStatisticsResponse;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class TravelStatisticsService {

    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final Clock clock;

    public TravelStatisticsService(
            TripRepository tripRepository,
            TripStopRepository tripStopRepository,
            Clock clock) {
        this.tripRepository = tripRepository;
        this.tripStopRepository = tripStopRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TravelStatisticsResponse calculateFor(UUID userId) {
        List<Trip> trips = tripRepository.findAccessibleTrips(userId);
        LocalDate today = LocalDate.now(clock);
        List<Trip> startedTrips = trips.stream()
                .filter(trip -> !trip.getStartDate().isAfter(today))
                .toList();
        List<UUID> startedTripIds = startedTrips.stream().map(Trip::getId).toList();
        long placesVisited = startedTripIds.isEmpty() ? 0 : tripStopRepository.countByTripIdIn(startedTripIds);
        long travelDays = startedTrips.stream()
                .mapToLong(trip -> ChronoUnit.DAYS.between(
                        trip.getStartDate(),
                        trip.getEndDate().isBefore(today) ? trip.getEndDate() : today) + 1)
                .sum();

        return new TravelStatisticsResponse(
                (int) startedTrips.stream().map(Trip::getCountryCode).distinct().count(),
                (int) startedTrips.stream().map(trip -> trip.getCountryCode() + ":" + trip.getCity()).distinct().count(),
                trips.size(),
                (int) trips.stream().filter(trip -> trip.getEndDate().isBefore(today)).count(),
                placesVisited,
                travelDays);
    }
}
