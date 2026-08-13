package com.travelmemory.invitation.dto;

import com.travelmemory.membership.entity.TripRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateInviteRequest(
        @NotNull TripRole role,
        @Min(1) @Max(30) int expiresInDays,
        @Min(1) @Max(100) int maxUses) {
}
