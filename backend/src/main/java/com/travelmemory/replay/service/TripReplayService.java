package com.travelmemory.replay.service;

import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.photo.entity.Photo;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.photo.service.PhotoStorageService;
import com.travelmemory.replay.dto.ReplayFrameResponse;
import com.travelmemory.replay.dto.ReplayPhotoResponse;
import com.travelmemory.replay.dto.TripReplayResponse;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStop;
import com.travelmemory.trip.repository.TripStopRepository;
import com.travelmemory.trip.service.TripService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TripReplayService {

    private static final int SECONDS_PER_SEGMENT = 3;

    private final TripService tripService;
    private final TripStopRepository tripStopRepository;
    private final PhotoRepository photoRepository;
    private final TripPermissionService tripPermissionService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final PhotoStorageService photoStorageService;
    private final RoadRouteService roadRouteService;

    public TripReplayService(
            TripService tripService,
            TripStopRepository tripStopRepository,
            PhotoRepository photoRepository,
            TripPermissionService tripPermissionService,
            AuthenticatedUserProvider authenticatedUserProvider,
            PhotoStorageService photoStorageService,
            RoadRouteService roadRouteService) {
        this.tripService = tripService;
        this.tripStopRepository = tripStopRepository;
        this.photoRepository = photoRepository;
        this.tripPermissionService = tripPermissionService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.photoStorageService = photoStorageService;
        this.roadRouteService = roadRouteService;
    }

    @Transactional(readOnly = true)
    public TripReplayResponse buildReplay(UUID tripId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        String accessToken = authenticatedUserProvider.getCurrentAccessToken();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireViewAccess(trip, userId);
        List<TripStop> stops = tripStopRepository.findByTripIdOrderByPositionAsc(tripId);
        Map<UUID, List<ReplayPhotoResponse>> photosByStop = groupPhotos(tripId, accessToken);

        List<ReplayFrameResponse> frames = java.util.stream.IntStream.range(0, stops.size())
                .mapToObj(index -> toFrame(trip, stops.get(index), index, photosByStop))
                .toList();
        RoadRouteResult route = roadRouteService.calculate(stops);
        return new TripReplayResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getStartDate(),
                trip.getEndDate(),
                route.totalDistanceKm(),
                Math.max(0, stops.size() - 1) * SECONDS_PER_SEGMENT,
                route.travelDurationSeconds(),
                route.source(),
                route.profile(),
                route.segments(),
                frames);
    }

    private Map<UUID, List<ReplayPhotoResponse>> groupPhotos(UUID tripId, String accessToken) {
        Map<UUID, List<ReplayPhotoResponse>> photosByStop = new HashMap<>();
        photoRepository.findByTripIdOrderByTakenAtAscCreatedAtAsc(tripId).stream()
                .filter(photo -> photo.getTripStop() != null)
                .forEach(photo -> photosByStop.computeIfAbsent(photo.getTripStop().getId(), ignored -> new java.util.ArrayList<>())
                        .add(toPhoto(photo, accessToken)));
        return photosByStop;
    }

    private ReplayFrameResponse toFrame(
            Trip trip,
            TripStop stop,
            int index,
            Map<UUID, List<ReplayPhotoResponse>> photosByStop) {
        int day = Math.toIntExact(ChronoUnit.DAYS.between(trip.getStartDate(), stop.getArrivalTime().toLocalDate()) + 1);
        return new ReplayFrameResponse(
                index,
                day,
                stop.getId(),
                stop.getName(),
                stop.getDescription(),
                stop.getCategory(),
                stop.getLatitude(),
                stop.getLongitude(),
                stop.getArrivalTime(),
                List.copyOf(photosByStop.getOrDefault(stop.getId(), List.of())));
    }

    private ReplayPhotoResponse toPhoto(Photo photo, String accessToken) {
        return new ReplayPhotoResponse(
                photo.getId(),
                photoStorageService.createSignedUrl(photo.getStoragePath(), accessToken),
                photo.getCaption(),
                photo.getTakenAt());
    }

}
