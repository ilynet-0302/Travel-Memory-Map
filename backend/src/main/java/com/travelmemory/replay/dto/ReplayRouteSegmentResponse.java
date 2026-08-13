package com.travelmemory.replay.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReplayRouteSegmentResponse(
        int sequence,
        UUID fromStopId,
        UUID toStopId,
        BigDecimal distanceKm,
        int durationSeconds,
        boolean followsRoads,
        List<ReplayCoordinateResponse> coordinates) {
}
