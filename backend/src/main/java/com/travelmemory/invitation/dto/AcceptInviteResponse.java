package com.travelmemory.invitation.dto;

import com.travelmemory.membership.entity.TripRole;

import java.util.UUID;

public record AcceptInviteResponse(UUID tripId, UUID memberId, TripRole role) {
}
