package com.travelmemory.replay.service;

import com.travelmemory.replay.dto.ReplayRouteSegmentResponse;
import com.travelmemory.replay.dto.ReplayRouteSource;

import java.math.BigDecimal;
import java.util.List;

public record RoadRouteResult(
        ReplayRouteSource source,
        String profile,
        BigDecimal totalDistanceKm,
        int travelDurationSeconds,
        List<ReplayRouteSegmentResponse> segments) {
}
