package com.travelmemory.trip.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.membership.repository.TripMemberRepository;
import com.travelmemory.membership.service.TripPermissionService;
import com.travelmemory.photo.repository.PhotoRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripStatus;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.mapper.TripMapper;
import com.travelmemory.trip.mapper.TripStopMapper;
import com.travelmemory.trip.repository.TripRepository;
import com.travelmemory.trip.repository.TripStopRepository;
import com.travelmemory.user.entity.UserProfile;
import com.travelmemory.user.service.UserProfileService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TripServiceTest {

    @Test
    void publicSlugExistsOnlyWhileTripIsPublic() {
        UserProfile owner = new UserProfile(UUID.randomUUID(), "owner@example.com", "Owner");
        Trip trip = new Trip(
                owner, "Rome", null, "Italy", "IT", "Rome",
                LocalDate.now().minusDays(5), LocalDate.now().minusDays(1), TripVisibility.PUBLIC);
        String firstSlug = trip.getPublicSlug();

        trip.updateDetails(
                "Rome", null, "Italy", "IT", "Rome",
                trip.getStartDate(), trip.getEndDate(), TripVisibility.PRIVATE);
        assertThat(trip.getPublicSlug()).isNull();

        trip.updateDetails(
                "Rome", null, "Italy", "IT", "Rome",
                trip.getStartDate(), trip.getEndDate(), TripVisibility.PUBLIC);
        assertThat(trip.getPublicSlug()).isNotBlank().isNotEqualTo(firstSlug);
    }

    @Test
    void archiveMarksTripArchivedAfterOwnerCheck() {
        TripRepository tripRepository = mock(TripRepository.class);
        TripMemberRepository memberRepository = mock(TripMemberRepository.class);
        TripStopRepository stopRepository = mock(TripStopRepository.class);
        AuthenticatedUserProvider userProvider = mock(AuthenticatedUserProvider.class);
        TripPermissionService permissionService = mock(TripPermissionService.class);
        TripService service = new TripService(
                tripRepository,
                memberRepository,
                stopRepository,
                mock(PhotoRepository.class),
                mock(UserProfileService.class),
                userProvider,
                permissionService,
                mock(TripMapper.class),
                mock(TripStopMapper.class));
        UserProfile owner = new UserProfile(UUID.randomUUID(), "owner@example.com", "Owner");
        Trip trip = new Trip(
                owner, "Rome", null, "Italy", "IT", "Rome",
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(8), TripVisibility.PRIVATE);
        AuthenticatedUser identity = new AuthenticatedUser(owner.getId(), owner.getEmail(), owner.getDisplayName());
        when(userProvider.getCurrentUser()).thenReturn(identity);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        service.archiveTrip(trip.getId());

        assertThat(trip.getStatus()).isEqualTo(TripStatus.ARCHIVED);
        verify(permissionService).requireOwner(trip, owner.getId());
    }
}
