package com.travelmemory.replay.service;

import com.travelmemory.replay.dto.ReplayCoordinateResponse;
import com.travelmemory.replay.dto.ReplayRouteSegmentResponse;
import com.travelmemory.replay.dto.ReplayRouteSource;
import com.travelmemory.trip.entity.TripStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class RoadRouteService {

    private static final Logger log = LoggerFactory.getLogger(RoadRouteService.class);
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final RestClient restClient;
    private final String baseUrl;
    private final String profile;
    private final boolean enabled;

    @Autowired
    public RoadRouteService(
            @Value("${app.routing.base-url}") String baseUrl,
            @Value("${app.routing.profile}") String profile,
            @Value("${app.routing.enabled:false}") boolean enabled,
            @Value("${app.routing.connect-timeout-seconds:3}") int connectTimeoutSeconds,
            @Value("${app.routing.read-timeout-seconds:8}") int readTimeoutSeconds) {
        this(
                createRestClient(connectTimeoutSeconds, readTimeoutSeconds),
                baseUrl,
                profile,
                enabled);
    }

    RoadRouteService(RestClient restClient, String baseUrl, String profile, boolean enabled) {
        this.restClient = restClient;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.profile = validateProfile(profile);
        this.enabled = enabled;
    }

    public RoadRouteResult calculate(List<TripStop> stops) {
        if (stops.size() < 2) {
            return new RoadRouteResult(
                    ReplayRouteSource.NO_ROUTE,
                    profile,
                    BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP),
                    0,
                    List.of());
        }
        if (!enabled) {
            return directFallback(stops);
        }

        try {
            OsrmResponse response = restClient.get()
                    .uri(routeUri(stops))
                    .retrieve()
                    .body(OsrmResponse.class);
            return toResult(response, stops);
        } catch (RestClientException | IllegalArgumentException exception) {
            log.warn("Road routing unavailable; using direct replay geometry", exception);
            return directFallback(stops);
        }
    }

    private RoadRouteResult toResult(OsrmResponse response, List<TripStop> stops) {
        if (response == null
                || !"Ok".equals(response.code())
                || response.routes() == null
                || response.routes().isEmpty()) {
            return directFallback(stops);
        }

        OsrmRoute route = response.routes().getFirst();
        if (route.legs() == null
                || route.legs().size() != stops.size() - 1
                || route.geometry() == null
                || route.geometry().coordinates() == null) {
            return directFallback(stops);
        }

        List<ReplayCoordinateResponse> routeCoordinates = route.geometry().coordinates().stream()
                .filter(position -> position != null && position.size() >= 2)
                .filter(position -> finite(position.get(0)) && finite(position.get(1)))
                .map(position -> new ReplayCoordinateResponse(
                        BigDecimal.valueOf(position.get(0)),
                        BigDecimal.valueOf(position.get(1))))
                .toList();
        if (routeCoordinates.size() < 2) {
            return directFallback(stops);
        }
        List<Integer> waypointIndexes = waypointIndexes(response.waypoints(), stops, routeCoordinates);

        List<ReplayRouteSegmentResponse> segments = new ArrayList<>();
        for (int index = 0; index < route.legs().size(); index++) {
            TripStop from = stops.get(index);
            TripStop to = stops.get(index + 1);
            OsrmLeg leg = route.legs().get(index);
            List<ReplayCoordinateResponse> coordinates = new ArrayList<>();
            addCoordinate(coordinates, coordinate(from));
            routeCoordinates.subList(waypointIndexes.get(index), waypointIndexes.get(index + 1) + 1)
                    .forEach(coordinate -> addCoordinate(coordinates, coordinate));
            addCoordinate(coordinates, coordinate(to));
            if (coordinates.size() < 2) {
                return directFallback(stops);
            }
            segments.add(new ReplayRouteSegmentResponse(
                    index,
                    from.getId(),
                    to.getId(),
                    kilometres(leg.distance()),
                    seconds(leg.duration()),
                    true,
                    List.copyOf(coordinates)));
        }

        double totalDistanceMetres = route.distance() == null
                ? route.legs().stream().mapToDouble(leg -> number(leg.distance())).sum()
                : route.distance();
        double totalDurationSeconds = route.duration() == null
                ? route.legs().stream().mapToDouble(leg -> number(leg.duration())).sum()
                : route.duration();
        return new RoadRouteResult(
                ReplayRouteSource.ROUTED,
                profile,
                kilometres(totalDistanceMetres),
                seconds(totalDurationSeconds),
                List.copyOf(segments));
    }

    private List<Integer> waypointIndexes(
            List<OsrmWaypoint> waypoints,
            List<TripStop> stops,
            List<ReplayCoordinateResponse> routeCoordinates) {
        List<Integer> indexes = new ArrayList<>();
        int previousIndex = 0;
        for (int index = 0; index < stops.size(); index++) {
            if (index == stops.size() - 1) {
                indexes.add(routeCoordinates.size() - 1);
                continue;
            }
            ReplayCoordinateResponse waypoint = waypointCoordinate(waypoints, stops.get(index), index);
            previousIndex = nearestCoordinateIndex(routeCoordinates, waypoint, previousIndex);
            indexes.add(previousIndex);
        }
        return indexes;
    }

    private ReplayCoordinateResponse waypointCoordinate(List<OsrmWaypoint> waypoints, TripStop stop, int index) {
        if (waypoints == null || index >= waypoints.size()) {
            return coordinate(stop);
        }
        List<Double> location = waypoints.get(index).location();
        if (location == null || location.size() < 2 || !finite(location.get(0)) || !finite(location.get(1))) {
            return coordinate(stop);
        }
        return new ReplayCoordinateResponse(
                BigDecimal.valueOf(location.get(0)),
                BigDecimal.valueOf(location.get(1)));
    }

    private int nearestCoordinateIndex(
            List<ReplayCoordinateResponse> routeCoordinates,
            ReplayCoordinateResponse waypoint,
            int startIndex) {
        int nearestIndex = startIndex;
        double nearestDistance = Double.MAX_VALUE;
        for (int index = startIndex; index < routeCoordinates.size(); index++) {
            double distance = squaredDistance(routeCoordinates.get(index), waypoint);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = index;
            }
        }
        return nearestIndex;
    }

    private double squaredDistance(ReplayCoordinateResponse left, ReplayCoordinateResponse right) {
        double longitudeDelta = left.longitude().doubleValue() - right.longitude().doubleValue();
        double latitudeDelta = left.latitude().doubleValue() - right.latitude().doubleValue();
        return longitudeDelta * longitudeDelta + latitudeDelta * latitudeDelta;
    }

    private RoadRouteResult directFallback(List<TripStop> stops) {
        List<ReplayRouteSegmentResponse> segments = new ArrayList<>();
        double totalDistance = 0;
        for (int index = 1; index < stops.size(); index++) {
            TripStop from = stops.get(index - 1);
            TripStop to = stops.get(index);
            double distance = haversine(from, to);
            totalDistance += distance;
            segments.add(new ReplayRouteSegmentResponse(
                    index - 1,
                    from.getId(),
                    to.getId(),
                    BigDecimal.valueOf(distance).setScale(1, RoundingMode.HALF_UP),
                    0,
                    false,
                    List.of(coordinate(from), coordinate(to))));
        }
        return new RoadRouteResult(
                ReplayRouteSource.DIRECT_FALLBACK,
                profile,
                BigDecimal.valueOf(totalDistance).setScale(1, RoundingMode.HALF_UP),
                0,
                List.copyOf(segments));
    }

    private URI routeUri(List<TripStop> stops) {
        String coordinates = stops.stream()
                .map(stop -> stop.getLongitude().toPlainString() + "," + stop.getLatitude().toPlainString())
                .collect(Collectors.joining(";"));
        return URI.create(baseUrl + "/route/v1/" + profile + "/" + coordinates
                + "?alternatives=false&steps=false&geometries=geojson&overview=full");
    }

    private ReplayCoordinateResponse coordinate(TripStop stop) {
        return new ReplayCoordinateResponse(stop.getLongitude(), stop.getLatitude());
    }

    private void addCoordinate(List<ReplayCoordinateResponse> coordinates, ReplayCoordinateResponse coordinate) {
        if (coordinates.isEmpty() || !sameCoordinate(coordinates.getLast(), coordinate)) {
            coordinates.add(coordinate);
        }
    }

    private boolean sameCoordinate(ReplayCoordinateResponse left, ReplayCoordinateResponse right) {
        return left.longitude().compareTo(right.longitude()) == 0
                && left.latitude().compareTo(right.latitude()) == 0;
    }

    private BigDecimal kilometres(Double metres) {
        return BigDecimal.valueOf(number(metres) / 1000).setScale(1, RoundingMode.HALF_UP);
    }

    private int seconds(Double duration) {
        return (int) Math.max(0, Math.round(number(duration)));
    }

    private double number(Double value) {
        return value == null || !Double.isFinite(value) ? 0 : value;
    }

    private boolean finite(Double value) {
        return value != null && Double.isFinite(value);
    }

    private double haversine(TripStop from, TripStop to) {
        double fromLatitude = Math.toRadians(from.getLatitude().doubleValue());
        double toLatitude = Math.toRadians(to.getLatitude().doubleValue());
        double latitudeDelta = toLatitude - fromLatitude;
        double longitudeDelta = Math.toRadians(to.getLongitude().doubleValue() - from.getLongitude().doubleValue());
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(fromLatitude) * Math.cos(toLatitude) * Math.pow(Math.sin(longitudeDelta / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(haversine));
    }

    private static RestClient createRestClient(int connectTimeoutSeconds, int readTimeoutSeconds) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        requestFactory.enableCompression(false);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "TravelMemoryMap/0.1 (portfolio project)")
                .build();
    }

    private static String validateProfile(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).trim();
        if (!normalized.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid routing profile");
        }
        return normalized;
    }

    private record OsrmResponse(String code, List<OsrmRoute> routes, List<OsrmWaypoint> waypoints) {
    }

    private record OsrmRoute(Double distance, Double duration, List<OsrmLeg> legs, OsrmGeometry geometry) {
    }

    private record OsrmLeg(Double distance, Double duration) {
    }

    private record OsrmWaypoint(List<Double> location) {
    }

    private record OsrmGeometry(List<List<Double>> coordinates) {
    }
}
