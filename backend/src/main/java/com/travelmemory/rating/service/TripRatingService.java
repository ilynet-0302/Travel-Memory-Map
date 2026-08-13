package com.travelmemory.rating.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.rating.dto.TripRatingSummaryResponse;
import com.travelmemory.rating.dto.TripRatingResponse;
import com.travelmemory.rating.dto.TripRatingBreakdownResponse;
import com.travelmemory.rating.dto.ReturnIntentSummaryResponse;
import com.travelmemory.rating.dto.UpsertTripRatingRequest;
import com.travelmemory.rating.entity.TripRating;
import com.travelmemory.rating.entity.WouldReturn;
import com.travelmemory.rating.repository.TripRatingRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.service.TripService;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.ToIntFunction;
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
    public TripRatingSummaryResponse rate(UUID tripId, UpsertTripRatingRequest request) {
        AuthenticatedUser authenticatedUser = authenticatedUserProvider.getCurrentUser();
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireMember(trip, authenticatedUser.id());
        TripRating rating = tripRatingRepository.findByTripIdAndUserId(tripId, authenticatedUser.id())
                .orElseGet(() -> {
                    UserProfile user = userProfileService.synchronizeProfile(authenticatedUser);
                    return new TripRating(
                            trip, user, request.food(), request.nightlife(), request.culture(), request.nature(),
                            request.walkability(), request.valueForMoney(), request.crowds(), request.relaxation(),
                            request.wouldReturn());
                });
        rating.updateDetails(
                request.food(), request.nightlife(), request.culture(), request.nature(), request.walkability(),
                request.valueForMoney(), request.crowds(), request.relaxation(), request.wouldReturn());
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
        List<TripRating> ratings = tripRatingRepository.findByTripId(trip.getId());
        TripRatingResponse currentUserRating = canRate
                ? ratings.stream()
                        .filter(rating -> rating.getUser().getId().equals(userId))
                        .findFirst()
                        .map(TripRatingResponse::from)
                        .orElse(null)
                : null;
        long yes = ratings.stream().filter(rating -> rating.getWouldReturn() == WouldReturn.YES).count();
        long maybe = ratings.stream().filter(rating -> rating.getWouldReturn() == WouldReturn.MAYBE).count();
        long no = ratings.stream().filter(rating -> rating.getWouldReturn() == WouldReturn.NO).count();
        return new TripRatingSummaryResponse(
                average(ratings, TripRating::getOverallScore),
                ratings.size(),
                currentUserRating,
                new TripRatingBreakdownResponse(
                        average(ratings, TripRating::getFood),
                        average(ratings, TripRating::getNightlife),
                        average(ratings, TripRating::getCulture),
                        average(ratings, TripRating::getNature),
                        average(ratings, TripRating::getWalkability),
                        average(ratings, TripRating::getValueForMoney),
                        average(ratings, TripRating::getCrowds),
                        average(ratings, TripRating::getRelaxation)),
                new ReturnIntentSummaryResponse(yes, maybe, no),
                canRate);
    }

    private BigDecimal average(List<TripRating> ratings, ToIntFunction<TripRating> value) {
        if (ratings.isEmpty()) {
            return null;
        }
        return BigDecimal.valueOf(ratings.stream().mapToInt(value).average().orElseThrow())
                .setScale(1, RoundingMode.HALF_UP);
    }
}
