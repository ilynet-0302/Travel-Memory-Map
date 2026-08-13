package com.travelmemory.invitation.dto;

import com.travelmemory.invitation.entity.InviteStatus;
import com.travelmemory.membership.entity.TripRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InviteResponse(
        UUID id,
        TripRole role,
        OffsetDateTime expiresAt,
        int maxUses,
        int useCount,
        InviteStatus status,
        OffsetDateTime createdAt) {
}
