package com.travelmemory.replay.dto;

import com.travelmemory.trip.entity.StopCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReplayFrameResponse(
        int sequence,
        int day,
        UUID stopId,
        String name,
        String description,
        StopCategory category,
        BigDecimal latitude,
        BigDecimal longitude,
        OffsetDateTime arrivalTime,
        List<ReplayPhotoResponse> photos) {
}

