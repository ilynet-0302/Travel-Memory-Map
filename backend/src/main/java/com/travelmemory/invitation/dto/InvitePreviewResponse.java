package com.travelmemory.invitation.dto;

import com.travelmemory.membership.entity.TripRole;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InvitePreviewResponse(
        UUID tripId,
        String tripTitle,
        String country,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        String invitedBy,
        TripRole role,
        OffsetDateTime expiresAt) {
}
