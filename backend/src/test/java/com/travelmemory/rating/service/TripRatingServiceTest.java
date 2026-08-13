package com.travelmemory.rating.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.rating.dto.TripRatingSummaryResponse;
import com.travelmemory.rating.entity.TripRating;
import com.travelmemory.rating.repository.TripRatingRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.service.TripService;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TripRatingServiceTest {

    private TripService tripService;
    private TripRatingRepository ratingRepository;
    private TripPermissionService permissionService;
    private AuthenticatedUserProvider userProvider;
    private UserProfileService profileService;
    private TripRatingService service;
    private UserProfile owner;
    private Trip trip;

    @BeforeEach
    void setUp() {
        tripService = mock(TripService.class);
        ratingRepository = mock(TripRatingRepository.class);
        permissionService = mock(TripPermissionService.class);
        userProvider = mock(AuthenticatedUserProvider.class);
        profileService = mock(UserProfileService.class);
        service = new TripRatingService(
                tripService, ratingRepository, permissionService, userProvider, profileService);
        owner = new UserProfile(UUID.randomUUID(), "owner@example.com", "Owner");
        trip = new Trip(
                owner,
                "Romanian road trip",
                null,
                "Romania",
                "RO",
                "Timisoara",
                LocalDate.of(2026, 8, 13),
                LocalDate.of(2026, 8, 20),
                TripVisibility.PRIVATE);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                owner.getId(), owner.getEmail(), owner.getDisplayName());
        when(userProvider.getCurrentUser()).thenReturn(authenticatedUser);
        when(profileService.synchronizeProfile(authenticatedUser)).thenReturn(owner);
        when(tripService.getTripEntity(trip.getId())).thenReturn(trip);
        when(permissionService.canViewMembers(trip, owner.getId())).thenReturn(true);
    }

    @Test
    void summaryRoundsTheAverageAndIncludesTheCurrentMemberScore() {
        TripRating ownRating = new TripRating(trip, owner, 9);
        when(ratingRepository.findByTripIdAndUserId(trip.getId(), owner.getId()))
                .thenReturn(Optional.of(ownRating));
        when(ratingRepository.averageScoreByTripId(trip.getId())).thenReturn(8.666666);
        when(ratingRepository.countByTripId(trip.getId())).thenReturn(3L);

        TripRatingSummaryResponse response = service.summary(trip.getId());

        assertThat(response.averageScore()).isEqualByComparingTo("8.7");
        assertThat(response.ratingCount()).isEqualTo(3);
        assertThat(response.currentUserScore()).isEqualTo(9);
        assertThat(response.canRate()).isTrue();
        verify(permissionService).requireViewAccess(trip, owner.getId());
    }

    @Test
    void rateCreatesOneRatingForTheCurrentTripMember() {
        AtomicReference<TripRating> saved = new AtomicReference<>();
        when(ratingRepository.findByTripIdAndUserId(trip.getId(), owner.getId()))
                .thenAnswer(invocation -> Optional.ofNullable(saved.get()));
        when(ratingRepository.save(any(TripRating.class))).thenAnswer(invocation -> {
            TripRating rating = invocation.getArgument(0);
            saved.set(rating);
            return rating;
        });
        when(ratingRepository.averageScoreByTripId(trip.getId())).thenReturn(10.0);
        when(ratingRepository.countByTripId(trip.getId())).thenReturn(1L);

        TripRatingSummaryResponse response = service.rate(trip.getId(), 10);

        assertThat(saved.get().getScore()).isEqualTo(10);
        assertThat(response.currentUserScore()).isEqualTo(10);
        verify(permissionService).requireMember(trip, owner.getId());
        verify(ratingRepository).save(saved.get());
    }

    @Test
    void rateUpdatesTheExistingRatingInsteadOfCreatingAnotherOne() {
        TripRating existing = new TripRating(trip, owner, 4);
        when(ratingRepository.findByTripIdAndUserId(trip.getId(), owner.getId()))
                .thenReturn(Optional.of(existing));
        when(ratingRepository.averageScoreByTripId(trip.getId())).thenReturn(7.0);
        when(ratingRepository.countByTripId(trip.getId())).thenReturn(2L);

        service.rate(trip.getId(), 7);

        assertThat(existing.getScore()).isEqualTo(7);
        verify(ratingRepository).save(existing);
    }

    @Test
    void removeDeletesOnlyTheCurrentMembersRating() {
        TripRating existing = new TripRating(trip, owner, 8);
        when(ratingRepository.findByTripIdAndUserId(trip.getId(), owner.getId()))
                .thenReturn(Optional.of(existing));

        service.remove(trip.getId());

        verify(permissionService).requireMember(trip, owner.getId());
        verify(ratingRepository).delete(existing);
    }
}
