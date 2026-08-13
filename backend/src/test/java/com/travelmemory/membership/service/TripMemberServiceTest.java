package com.travelmemory.membership.service;

import com.travelmemory.auth.AuthenticatedUser;
import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.exception.CannotRemoveTripOwnerException;
import com.travelmemory.exception.InvalidTripRoleException;
import com.travelmemory.exception.OwnerCannotLeaveTripException;
import com.travelmemory.membership.dto.TripMemberResponse;
import com.travelmemory.membership.dto.UpdateMemberRoleRequest;
import com.travelmemory.membership.entity.TripMember;
import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.membership.repository.TripMemberRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.entity.TripVisibility;
import com.travelmemory.trip.service.TripService;
import com.travelmemory.user.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TripMemberServiceTest {

    private TripMemberRepository memberRepository;
    private TripPermissionService permissionService;
    private AuthenticatedUserProvider userProvider;
    private TripMemberService service;
    private UserProfile owner;
    private Trip trip;
    private AuthenticatedUser ownerIdentity;

    @BeforeEach
    void setUp() {
        TripService tripService = mock(TripService.class);
        memberRepository = mock(TripMemberRepository.class);
        permissionService = mock(TripPermissionService.class);
        userProvider = mock(AuthenticatedUserProvider.class);
        service = new TripMemberService(tripService, memberRepository, permissionService, userProvider);
        owner = new UserProfile(UUID.randomUUID(), "owner@example.com", "Owner");
        ownerIdentity = new AuthenticatedUser(owner.getId(), owner.getEmail(), owner.getDisplayName());
        trip = new Trip(
                owner, "Rome", null, "Italy", "IT", "Rome",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), TripVisibility.PRIVATE);
        when(tripService.getTripEntity(trip.getId())).thenReturn(trip);
        when(userProvider.getCurrentUser()).thenReturn(ownerIdentity);
    }

    @Test
    void ownerCanChangeEditorToViewer() {
        TripMember editor = member(TripRole.EDITOR);
        when(memberRepository.findByIdAndTripId(editor.getId(), trip.getId())).thenReturn(Optional.of(editor));

        TripMemberResponse response = service.updateMemberRole(
                trip.getId(), editor.getId(), new UpdateMemberRoleRequest(TripRole.VIEWER));

        assertThat(response.role()).isEqualTo(TripRole.VIEWER);
        verify(permissionService).requireOwner(trip, owner.getId());
    }

    @Test
    void memberListRequiresMembershipInsteadOfPublicVisibility() {
        service.listMembers(trip.getId());

        verify(permissionService).requireMember(trip, owner.getId());
    }

    @Test
    void cannotPromoteMemberToOwner() {
        TripMember editor = member(TripRole.EDITOR);

        assertThatThrownBy(() -> service.updateMemberRole(
                trip.getId(), editor.getId(), new UpdateMemberRoleRequest(TripRole.OWNER)))
                .isInstanceOf(InvalidTripRoleException.class);
        verify(memberRepository, never()).findByIdAndTripId(editor.getId(), trip.getId());
    }

    @Test
    void ownerCannotBeRemoved() {
        TripMember ownerMembership = new TripMember(trip, owner, TripRole.OWNER);
        when(memberRepository.findByIdAndTripId(ownerMembership.getId(), trip.getId()))
                .thenReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() -> service.removeMember(trip.getId(), ownerMembership.getId()))
                .isInstanceOf(CannotRemoveTripOwnerException.class);
        verify(memberRepository, never()).delete(ownerMembership);
    }

    @Test
    void nonOwnerCanLeaveTrip() {
        UserProfile editor = new UserProfile(UUID.randomUUID(), "editor@example.com", "Editor");
        TripMember membership = new TripMember(trip, editor, TripRole.EDITOR);
        when(userProvider.getCurrentUser())
                .thenReturn(new AuthenticatedUser(editor.getId(), editor.getEmail(), editor.getDisplayName()));
        when(memberRepository.findByTripIdAndUserId(trip.getId(), editor.getId()))
                .thenReturn(Optional.of(membership));

        service.leaveTrip(trip.getId());

        verify(memberRepository).delete(membership);
    }

    @Test
    void ownerCannotLeaveTrip() {
        TripMember membership = new TripMember(trip, owner, TripRole.OWNER);
        when(memberRepository.findByTripIdAndUserId(trip.getId(), owner.getId()))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.leaveTrip(trip.getId()))
                .isInstanceOf(OwnerCannotLeaveTripException.class);
        verify(memberRepository, never()).delete(membership);
    }

    private TripMember member(TripRole role) {
        UserProfile user = new UserProfile(UUID.randomUUID(), "member@example.com", "Member");
        return new TripMember(trip, user, role);
    }
}
