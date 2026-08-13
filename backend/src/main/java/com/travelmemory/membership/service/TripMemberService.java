package com.travelmemory.membership.service;

import com.travelmemory.auth.AuthenticatedUserProvider;
import com.travelmemory.exception.CannotRemoveTripOwnerException;
import com.travelmemory.exception.InvalidTripRoleException;
import com.travelmemory.exception.OwnerCannotLeaveTripException;
import com.travelmemory.exception.TripMemberNotFoundException;
import com.travelmemory.membership.dto.TripMemberResponse;
import com.travelmemory.membership.dto.UpdateMemberRoleRequest;
import com.travelmemory.membership.entity.TripMember;
import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.membership.repository.TripMemberRepository;
import com.travelmemory.trip.entity.Trip;
import com.travelmemory.trip.service.TripService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TripMemberService {

    private final TripService tripService;
    private final TripMemberRepository tripMemberRepository;
    private final TripPermissionService tripPermissionService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public TripMemberService(
            TripService tripService,
            TripMemberRepository tripMemberRepository,
            TripPermissionService tripPermissionService,
            AuthenticatedUserProvider authenticatedUserProvider) {
        this.tripService = tripService;
        this.tripMemberRepository = tripMemberRepository;
        this.tripPermissionService = tripPermissionService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Transactional(readOnly = true)
    public List<TripMemberResponse> listMembers(UUID tripId) {
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireMember(trip, authenticatedUserProvider.getCurrentUser().id());
        return tripMemberRepository.findByTripIdOrderByJoinedAtAsc(tripId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TripMemberResponse updateMemberRole(UUID tripId, UUID memberId, UpdateMemberRoleRequest request) {
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireOwner(trip, authenticatedUserProvider.getCurrentUser().id());
        if (request.role() == TripRole.OWNER) {
            throw new InvalidTripRoleException("Member management can assign only EDITOR or VIEWER access.");
        }
        TripMember member = findMember(tripId, memberId);
        member.changeRole(request.role());
        return toResponse(member);
    }

    @Transactional
    public void removeMember(UUID tripId, UUID memberId) {
        Trip trip = tripService.getTripEntity(tripId);
        tripPermissionService.requireOwner(trip, authenticatedUserProvider.getCurrentUser().id());
        TripMember member = findMember(tripId, memberId);
        if (member.getRole() == TripRole.OWNER) {
            throw new CannotRemoveTripOwnerException();
        }
        tripMemberRepository.delete(member);
    }

    @Transactional
    public void leaveTrip(UUID tripId) {
        UUID userId = authenticatedUserProvider.getCurrentUser().id();
        TripMember member = tripMemberRepository.findByTripIdAndUserId(tripId, userId)
                .orElseThrow(TripMemberNotFoundException::new);
        if (member.getRole() == TripRole.OWNER) {
            throw new OwnerCannotLeaveTripException();
        }
        tripMemberRepository.delete(member);
    }

    private TripMember findMember(UUID tripId, UUID memberId) {
        return tripMemberRepository.findByIdAndTripId(memberId, tripId)
                .orElseThrow(TripMemberNotFoundException::new);
    }

    private TripMemberResponse toResponse(TripMember member) {
        return new TripMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getDisplayName(),
                member.getUser().getAvatarUrl(),
                member.getRole(),
                member.getJoinedAt());
    }
}
