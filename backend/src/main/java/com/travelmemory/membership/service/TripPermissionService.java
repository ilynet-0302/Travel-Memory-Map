package com.travelmemory.membership.service;

import com.travelmemory.exception.TripAccessDeniedException;
import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.membership.repository.TripMemberRepository;
import com.travelmemory.trip.entity.Trip;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.UUID;

@Service
public class TripPermissionService {

    private final TripMemberRepository tripMemberRepository;

    public TripPermissionService(TripMemberRepository tripMemberRepository) {
        this.tripMemberRepository = tripMemberRepository;
    }

    public boolean canViewTrip(Trip trip, UUID userId) {
        return trip.getOwner().getId().equals(userId)
                || tripMemberRepository.existsByTripIdAndUserId(trip.getId(), userId);
    }

    public boolean canEditTripContent(Trip trip, UUID userId) {
        return hasAnyRole(trip, userId, EnumSet.of(TripRole.OWNER, TripRole.EDITOR));
    }

    public boolean canManageMembers(Trip trip, UUID userId) {
        return hasAnyRole(trip, userId, EnumSet.of(TripRole.OWNER));
    }

    public boolean canViewMembers(Trip trip, UUID userId) {
        return trip.getOwner().getId().equals(userId)
                || tripMemberRepository.existsByTripIdAndUserId(trip.getId(), userId);
    }

    public void requireViewAccess(Trip trip, UUID userId) {
        if (!canViewTrip(trip, userId)) {
            throw new TripAccessDeniedException();
        }
    }

    public void requireEditorOrOwner(Trip trip, UUID userId) {
        if (!canEditTripContent(trip, userId)) {
            throw new TripAccessDeniedException();
        }
    }

    public void requireOwner(Trip trip, UUID userId) {
        if (!hasAnyRole(trip, userId, EnumSet.of(TripRole.OWNER))) {
            throw new TripAccessDeniedException();
        }
    }

    public void requireMember(Trip trip, UUID userId) {
        if (!canViewMembers(trip, userId)) {
            throw new TripAccessDeniedException();
        }
    }

    private boolean hasAnyRole(Trip trip, UUID userId, EnumSet<TripRole> allowedRoles) {
        return tripMemberRepository.findByTripIdAndUserId(trip.getId(), userId)
                .map(member -> allowedRoles.contains(member.getRole()))
                .orElse(false);
    }
}
