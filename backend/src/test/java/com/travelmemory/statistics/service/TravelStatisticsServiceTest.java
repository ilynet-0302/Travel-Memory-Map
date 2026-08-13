package com.travelmemory.statistics.service;

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

    private TravelStatisticsService travelStatisticsService;

    @BeforeEach
    void setUp() {
        travelStatisticsService = new TravelStatisticsService(
                tripRepository,
                tripStopRepository,
                photoRepository,
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

        TravelStatisticsResponse statistics = travelStatisticsService.calculateFor(userId);

        assertThat(statistics.countriesVisited()).isEqualTo(1);
        assertThat(statistics.citiesVisited()).isEqualTo(1);
        assertThat(statistics.trips()).isEqualTo(2);
        assertThat(statistics.completedTrips()).isEqualTo(1);
        assertThat(statistics.placesVisited()).isEqualTo(7);
        assertThat(statistics.photosUploaded()).isEqualTo(18);
        assertThat(statistics.travelDays()).isEqualTo(5);
    }
}
