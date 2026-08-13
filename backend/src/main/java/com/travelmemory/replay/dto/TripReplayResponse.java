package com.travelmemory.replay.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TripReplayResponse(
        UUID tripId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalDistanceKm,
        int estimatedDurationSeconds,
        int travelDurationSeconds,
        ReplayRouteSource routeSource,
        String routeProfile,
        List<ReplayRouteSegmentResponse> routeSegments,
        List<ReplayFrameResponse> frames) {
}
