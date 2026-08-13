package com.travelmemory.replay.service;

import com.travelmemory.replay.dto.ReplayRouteSource;
import com.travelmemory.trip.entity.StopCategory;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStop;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.user.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RoadRouteServiceTest {

    private MockRestServiceServer server;
    private RoadRouteService routeService;
    private Trip trip;
    private UserProfile owner;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        routeService = new RoadRouteService(builder.build(), "https://router.example", "driving", true);
        owner = new UserProfile(UUID.randomUUID(), "owner@example.com", "Owner");
        trip = new Trip(
                owner, "Road trip", null, "Romania", "RO", "Timisoara",
                LocalDate.of(2026, 8, 18), LocalDate.of(2026, 8, 20), TripVisibility.PRIVATE);
    }

    @Test
    void returnsRoadGeometryForEveryLegInStopOrder() {
        TripStop first = stop("First", 45.7304, 21.2382, 0);
        TripStop second = stop("Second", 45.7400, 21.2500, 1);
        TripStop third = stop("Third", 45.7500, 21.2700, 2);
        server.expect(requestTo(containsString("/route/v1/driving/21.2382,45.7304;21.25,45.74;21.27,45.75")))
                .andRespond(withSuccess("""
                        {
                          "code": "Ok",
                          "waypoints": [
                            {"location": [21.2382,45.7304]},
                            {"location": [21.2500,45.7400]},
                            {"location": [21.2700,45.7500]}
                          ],
                          "routes": [{
                            "distance": 3100.0,
                            "duration": 420.0,
                            "geometry": {"coordinates": [
                              [21.2382,45.7304],
                              [21.2440,45.7350],
                              [21.2500,45.7400],
                              [21.2600,45.7440],
                              [21.2700,45.7500]
                            ]},
                            "legs": [
                              {
                                "distance": 1200.0,
                                "duration": 160.0
                              },
                              {
                                "distance": 1900.0,
                                "duration": 260.0
                              }
                            ]
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        RoadRouteResult result = routeService.calculate(List.of(first, second, third));

        assertThat(result.source()).isEqualTo(ReplayRouteSource.ROUTED);
        assertThat(result.totalDistanceKm()).isEqualByComparingTo("3.1");
        assertThat(result.travelDurationSeconds()).isEqualTo(420);
        assertThat(result.segments()).hasSize(2);
        assertThat(result.segments()).allMatch(segment -> segment.followsRoads());
        assertThat(result.segments().getFirst().coordinates()).hasSize(3);
        assertThat(result.segments().getFirst().coordinates().get(1).longitude()).isEqualByComparingTo("21.244");
        server.verify();
    }

    @Test
    void fallsBackToDirectSegmentsWhenNoRoadRouteExists() {
        TripStop first = stop("First", 45.7304, 21.2382, 0);
        TripStop second = stop("Second", 45.7400, 21.2500, 1);
        server.expect(requestTo(containsString("/route/v1/driving/")))
                .andRespond(withSuccess("{\"code\":\"NoRoute\",\"routes\":[]}", MediaType.APPLICATION_JSON));

        RoadRouteResult result = routeService.calculate(List.of(first, second));

        assertThat(result.source()).isEqualTo(ReplayRouteSource.DIRECT_FALLBACK);
        assertThat(result.segments()).singleElement().satisfies(segment -> {
            assertThat(segment.followsRoads()).isFalse();
            assertThat(segment.coordinates()).hasSize(2);
        });
        assertThat(result.totalDistanceKm()).isPositive();
        server.verify();
    }

    @Test
    void keepsPrivateCoordinatesLocalUntilRoutingIsExplicitlyEnabled() {
        RoadRouteService disabledService = new RoadRouteService(
                RestClient.create(), "https://router.example", "driving", false);
        TripStop first = stop("First", 45.7304, 21.2382, 0);
        TripStop second = stop("Second", 45.7400, 21.2500, 1);

        RoadRouteResult result = disabledService.calculate(List.of(first, second));

        assertThat(result.source()).isEqualTo(ReplayRouteSource.DIRECT_FALLBACK);
        assertThat(result.segments()).singleElement().satisfies(segment -> assertThat(segment.followsRoads()).isFalse());
    }

    private TripStop stop(String name, double latitude, double longitude, int position) {
        return new TripStop(
                trip,
                owner,
                name,
                null,
                BigDecimal.valueOf(latitude),
                BigDecimal.valueOf(longitude),
                OffsetDateTime.of(2026, 8, 18 + position, 10, 0, 0, 0, ZoneOffset.UTC),
                null,
                StopCategory.LANDMARK,
                null,
                position);
    }
}
