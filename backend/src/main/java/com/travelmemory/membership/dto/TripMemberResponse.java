package com.travelmemory.membership.dto;

import com.travelmemory.membership.entity.TripRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TripMemberResponse(
        UUID memberId,
        UUID userId,
        String displayName,
        String avatarUrl,
        TripRole role,
        OffsetDateTime joinedAt) {
}
