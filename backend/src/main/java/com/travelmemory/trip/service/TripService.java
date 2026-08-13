package com.travelmemory.trip.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.exception.InvalidTripDateException;
import com.travelmemory.exception.TripNotFoundException;
import com.travelmemory.membership.entity.TripMember;
import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.membership.repository.TripMemberRepository;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.trip.dto.CreateTripRequest;
import com.travelmemory.trip.dto.TripDetailsResponse;
import com.travelmemory.trip.dto.TripStopResponse;
import com.travelmemory.trip.dto.TripSummaryResponse;
import com.travelmemory.trip.dto.UpdateTripRequest;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.mapper.TripMapper;
import com.travelmemory.trip.mapper.TripStopMapper;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripStopRepository tripStopRepository;
    private final UserProfileService userProfileService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final TripPermissionService tripPermissionService;
    private final TripMapper tripMapper;
    private final TripStopMapper tripStopMapper;

    public TripService(
            TripRepository tripRepository,
            TripMemberRepository tripMemberRepository,
            TripStopRepository tripStopRepository,
            UserProfileService userProfileService,
            AuthenticatedUserProvider authenticatedUserProvider,
            TripPermissionService tripPermissionService,
            TripMapper tripMapper,
            TripStopMapper tripStopMapper) {
        this.tripRepository = tripRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.tripStopRepository = tripStopRepository;
        this.userProfileService = userProfileService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.tripPermissionService = tripPermissionService;
        this.tripMapper = tripMapper;
        this.tripStopMapper = tripStopMapper;
    }

    @Transactional
    public TripDetailsResponse createTrip(CreateTripRequest request) {
        validateDateRange(request.startDate(), request.endDate());
        AuthenticatedUser authenticatedUser = authenticatedUserProvider.getCurrentUser();
        UserProfile owner = userProfileService.synchronizeProfile(authenticatedUser);
        Trip trip = tripRepository.save(new Trip(
                owner,
                request.title(),
                request.description(),
                request.country(),
                normalizeCountryCode(request.countryCode(), request.country()),
                request.city(),
                request.startDate(),
                request.endDate(),
                request.visibility()));
        tripMemberRepository.save(new TripMember(trip, owner, TripRole.OWNER));
        return tripMapper.toDetails(trip, TripRole.OWNER, 1, List.of());
    }

    @Transactional(readOnly = true)
    public List<TripSummaryResponse> listTrips() {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        return tripRepository.findAccessibleTrips(userId).stream()
                .map(trip -> tripMapper.toSummary(
                        trip,
                        currentUserRole(trip.getId(), userId),
                        tripMemberRepository.countByTripId(trip.getId()),
                        tripStopRepository.countByTripId(trip.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TripDetailsResponse getTrip(UUID tripId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = getTripEntity(tripId);
        tripPermissionService.requireViewAccess(trip, userId);
        List<TripStopResponse> stops = tripStopRepository.findByTripIdOrderByPositionAsc(tripId).stream()
                .map(tripStopMapper::toResponse)
                .toList();
        return tripMapper.toDetails(
                trip, currentUserRole(tripId, userId), tripMemberRepository.countByTripId(tripId), stops);
    }

    @Transactional
    public TripDetailsResponse updateTrip(UUID tripId, UpdateTripRequest request) {
        validateDateRange(request.startDate(), request.endDate());
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = getTripEntity(tripId);
        tripPermissionService.requireOwner(trip, userId);
        trip.updateDetails(
                request.title(),
                request.description(),
                request.country(),
                normalizeCountryCode(request.countryCode(), request.country()),
                request.city(),
                request.startDate(),
                request.endDate(),
                request.visibility());
        List<TripStopResponse> stops = tripStopRepository.findByTripIdOrderByPositionAsc(tripId).stream()
                .map(tripStopMapper::toResponse)
                .toList();
        return tripMapper.toDetails(
                trip, currentUserRole(tripId, userId), tripMemberRepository.countByTripId(tripId), stops);
    }

    @Transactional
    public void deleteTrip(UUID tripId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = getTripEntity(tripId);
        tripPermissionService.requireOwner(trip, userId);
        tripRepository.delete(trip);
    }

    @Transactional
    public void archiveTrip(UUID tripId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = getTripEntity(tripId);
        tripPermissionService.requireOwner(trip, userId);
        trip.archive();
    }

    @Transactional(readOnly = true)
    public Trip getTripEntity(UUID tripId) {
        return tripRepository.findById(tripId).orElseThrow(() -> new TripNotFoundException(tripId));
    }

    private void validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new InvalidTripDateException("Trip end date must be on or after its start date.");
        }
    }

    private TripRole currentUserRole(UUID tripId, UUID userId) {
        return tripMemberRepository.findByTripIdAndUserId(tripId, userId)
                .map(TripMember::getRole)
                .orElse(null);
    }

    private String normalizeCountryCode(String countryCode, String country) {
        if (countryCode != null && !countryCode.isBlank()) {
            return countryCode.toUpperCase(Locale.ROOT);
        }
        String letters = country.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        return letters.length() >= 2 ? letters.substring(0, 2) : "XX";
    }
}
