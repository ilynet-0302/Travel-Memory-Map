package com.travelmemory.user.dto;

import com.travelmemory.statistics.dto.TravelStatisticsResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        TravelStatisticsResponse statistics) {
}
