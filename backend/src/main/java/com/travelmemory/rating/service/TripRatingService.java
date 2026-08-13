package com.travelmemory.rating.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.rating.dto.TripRatingSummaryResponse;
import com.travelmemory.rating.entity.TripRating;
import com.travelmemory.rating.repository.TripRatingRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.service.TripService;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class TripRatingService {

    private final TripService tripService;
    private final TripRatingRepository tripRatingRepository;
    private final TripPermissionService tripPermissionService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final UserProfileService userProfileService;

    public TripRatingService(
            TripService tripService,
            TripRatingRepository tripRatingRepository,
            TripPermissionService tripPermissionService,
            AuthenticatedUserProvider authenticatedUserProvider,
            UserProfileService userProfileService) {
        this.tripService = tripService;
        this.tripRatingRepository = tripRatingRepository;
        this.tripPermissionService = tripPermissionService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.userProfileService = userProfileService;
    }

    @Transactional(readOnly = true)
    public TripRatingSummaryResponse summary(UUID tripId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireViewAccess(trip, userId);
        return summarize(trip, userId);
    }

    @Transactional
    public TripRatingSummaryResponse rate(UUID tripId, int score) {
        AuthenticatedUser authenticatedUser = authenticatedUserProvider.getCurrentUser();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireMember(trip, authenticatedUser.id());
        TripRating rating = tripRatingRepository.findByTripIdAndUserId(tripId, authenticatedUser.id())
                .orElseGet(() -> {
                    UserProfile user = userProfileService.synchronizeProfile(authenticatedUser);
                    return new TripRating(trip, user, score);
                });
        rating.updateScore(score);
        tripRatingRepository.save(rating);
        return summarize(trip, authenticatedUser.id());
    }

    @Transactional
    public void remove(UUID tripId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireMember(trip, userId);
        tripRatingRepository.findByTripIdAndUserId(tripId, userId)
                .ifPresent(tripRatingRepository::delete);
    }

    private TripRatingSummaryResponse summarize(Trip trip, UUID userId) {
        boolean canRate = tripPermissionService.canViewMembers(trip, userId);
        Integer currentUserScore = canRate
                ? tripRatingRepository.findByTripIdAndUserId(trip.getId(), userId)
                        .map(TripRating::getScore)
                        .orElse(null)
                : null;
        Double average = tripRatingRepository.averageScoreByTripId(trip.getId());
        return new TripRatingSummaryResponse(
                average == null ? null : BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP),
                tripRatingRepository.countByTripId(trip.getId()),
                currentUserScore,
                canRate);
    }
}
