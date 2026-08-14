package com.travelmemory.trip.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.exception.InvalidTripDateException;
import com.travelmemory.exception.InvalidTripStopOrderException;
import com.travelmemory.exception.TripStopNotFoundException;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.trip.dto.CreateTripStopRequest;
import com.travelmemory.trip.dto.ReorderTripStopsRequest;
import com.travelmemory.trip.dto.TripStopResponse;
import com.travelmemory.trip.dto.UpdateTripStopRequest;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStop;
import com.travelmemory.trip.mapper.TripStopMapper;
import com.travelmemory.trip.repository.TripStopRepository;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TripStopService {

    private final TripService tripService;
    private final TripStopRepository tripStopRepository;
    private final TripPermissionService tripPermissionService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final UserProfileService userProfileService;
    private final TripStopMapper tripStopMapper;

    public TripStopService(
            TripService tripService,
            TripStopRepository tripStopRepository,
            TripPermissionService tripPermissionService,
            AuthenticatedUserProvider authenticatedUserProvider,
            UserProfileService userProfileService,
            TripStopMapper tripStopMapper) {
        this.tripService = tripService;
        this.tripStopRepository = tripStopRepository;
        this.tripPermissionService = tripPermissionService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.userProfileService = userProfileService;
        this.tripStopMapper = tripStopMapper;
    }

    @Transactional
    public TripStopResponse addStop(UUID tripId, CreateTripStopRequest request) {
        AuthenticatedUser authenticatedUser = authenticatedUserProvider.getCurrentUser();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireEditorOrOwner(trip, authenticatedUser.id());
        validateStopTime(trip, request.arrivalTime(), request.departureTime());
        UserProfile creator = userProfileService.synchronizeProfile(authenticatedUser);
        TripStop stop = tripStopRepository.save(new TripStop(
                trip,
                creator,
                request.name(),
                request.description(),
                request.latitude(),
                request.longitude(),
                request.arrivalTime(),
                request.departureTime(),
                request.category(),
                request.rating(),
                request.position()));
        return tripStopMapper.toResponse(stop);
    }

    @Transactional
    public TripStopResponse updateStop(UUID tripId, UUID stopId, UpdateTripStopRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireEditorOrOwner(trip, userId);
        validateStopTime(trip, request.arrivalTime(), request.departureTime());
        TripStop stop = findStop(tripId, stopId);
        stop.updateDetails(
                request.name(),
                request.description(),
                request.latitude(),
                request.longitude(),
                request.arrivalTime(),
                request.departureTime(),
                request.category(),
                request.rating(),
                request.position());
        return tripStopMapper.toResponse(stop);
    }

    @Transactional
    public void deleteStop(UUID tripId, UUID stopId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireEditorOrOwner(trip, userId);
        tripStopRepository.delete(findStop(tripId, stopId));
    }

    @Transactional
    public List<TripStopResponse> reorderStops(UUID tripId, ReorderTripStopsRequest request) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireEditorOrOwner(trip, userId);

        List<TripStop> stops = tripStopRepository.findByTripIdForUpdate(tripId);
        List<UUID> requestedIds = request.stopIds();
        if (requestedIds.size() != stops.size()
                || new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new InvalidTripStopOrderException();
        }

        Map<UUID, TripStop> stopsById = stops.stream()
                .collect(Collectors.toMap(TripStop::getId, Function.identity()));
        if (!stopsById.keySet().containsAll(requestedIds)) {
            throw new InvalidTripStopOrderException();
        }

        for (int position = 0; position < requestedIds.size(); position++) {
            stopsById.get(requestedIds.get(position)).moveToPosition(position);
        }

        return requestedIds.stream()
                .map(stopsById::get)
                .map(tripStopMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TripStopResponse> listStops(UUID tripId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireViewAccess(trip, userId);
        return tripStopRepository.findByTripIdOrderByPositionAsc(tripId).stream()
                .map(tripStopMapper::toResponse)
                .toList();
    }

    private TripStop findStop(UUID tripId, UUID stopId) {
        return tripStopRepository.findByIdAndTripId(stopId, tripId)
                .orElseThrow(() -> new TripStopNotFoundException(stopId));
    }

    private void validateStopTime(Trip trip, java.time.OffsetDateTime arrivalTime, java.time.OffsetDateTime departureTime) {
        if (departureTime != null && departureTime.isBefore(arrivalTime)) {
            throw new InvalidTripDateException("Stop departure time cannot be before arrival time.");
        }
        LocalDate arrivalDate = arrivalTime.toLocalDate();
        if (arrivalDate.isBefore(trip.getStartDate()) || arrivalDate.isAfter(trip.getEndDate())) {
            throw new InvalidTripDateException("Trip stop must fall within the trip date range.");
        }
        if (departureTime != null) {
            LocalDate departureDate = departureTime.toLocalDate();
            if (departureDate.isBefore(trip.getStartDate()) || departureDate.isAfter(trip.getEndDate())) {
                throw new InvalidTripDateException("Trip stop must fall within the trip date range.");
            }
        }
    }
}
