package com.travelmemory.membership.service;

import com.travelmemory.exception.TripAccessDeniedException;
import com.travelmemory.membership.entity.TripMember;
import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.membership.repository.TripMemberRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.user.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TripPermissionServiceTest {

    private TripMemberRepository tripMemberRepository;
    private TripPermissionService permissionService;
    private UserProfile owner;

    @BeforeEach
    void setUp() {
        tripMemberRepository = mock(TripMemberRepository.class);
        permissionService = new TripPermissionService(tripMemberRepository);
        owner = new UserProfile(UUID.randomUUID(), "owner@example.com", "Owner");
    }

    @Test
    void privateTripIsNotVisibleToStranger() {
        Trip trip = trip(TripVisibility.PRIVATE);
        UUID strangerId = UUID.randomUUID();
        when(tripMemberRepository.existsByTripIdAndUserId(trip.getId(), strangerId)).thenReturn(false);

        assertThat(permissionService.canViewTrip(trip, strangerId)).isFalse();
        assertThatThrownBy(() -> permissionService.requireViewAccess(trip, strangerId))
                .isInstanceOf(TripAccessDeniedException.class);
    }

    @Test
    void publicTripIsVisibleWithoutMembership() {
        Trip trip = trip(TripVisibility.PUBLIC);

        assertThat(permissionService.canViewTrip(trip, UUID.randomUUID())).isTrue();
    }

    @Test
    void publicTripDoesNotExposeMemberListToStranger() {
        Trip trip = trip(TripVisibility.PUBLIC);
        UUID strangerId = UUID.randomUUID();
        when(tripMemberRepository.existsByTripIdAndUserId(trip.getId(), strangerId)).thenReturn(false);

        assertThat(permissionService.canViewMembers(trip, strangerId)).isFalse();
        assertThatThrownBy(() -> permissionService.requireMember(trip, strangerId))
                .isInstanceOf(TripAccessDeniedException.class);
    }

    @Test
    void viewerCannotEditTripContent() {
        Trip trip = trip(TripVisibility.PRIVATE);
        UserProfile viewer = new UserProfile(UUID.randomUUID(), "viewer@example.com", "Viewer");
        when(tripMemberRepository.findByTripIdAndUserId(trip.getId(), viewer.getId()))
                .thenReturn(Optional.of(new TripMember(trip, viewer, TripRole.VIEWER)));

        assertThat(permissionService.canEditTripContent(trip, viewer.getId())).isFalse();
        assertThatThrownBy(() -> permissionService.requireEditorOrOwner(trip, viewer.getId()))
                .isInstanceOf(TripAccessDeniedException.class);
    }

    @Test
    void editorCanEditTripContentButCannotManageMembers() {
        Trip trip = trip(TripVisibility.PRIVATE);
        UserProfile editor = new UserProfile(UUID.randomUUID(), "editor@example.com", "Editor");
        when(tripMemberRepository.findByTripIdAndUserId(trip.getId(), editor.getId()))
                .thenReturn(Optional.of(new TripMember(trip, editor, TripRole.EDITOR)));

        assertThat(permissionService.canEditTripContent(trip, editor.getId())).isTrue();
        assertThat(permissionService.canManageMembers(trip, editor.getId())).isFalse();
        assertThatThrownBy(() -> permissionService.requireOwner(trip, editor.getId()))
                .isInstanceOf(TripAccessDeniedException.class);
    }

    private Trip trip(TripVisibility visibility) {
        return new Trip(
                owner,
                "Rome",
                "Five days in Rome",
                "Italy",
                "IT",
                "Rome",
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                visibility);
    }
}
