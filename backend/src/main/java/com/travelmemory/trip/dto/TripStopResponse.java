package com.travelmemory.trip.dto;

import com.travelmemory.trip.entity.StopCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TripStopResponse(
        UUID id,
        UUID createdByUserId,
        String name,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        OffsetDateTime arrivalTime,
        OffsetDateTime departureTime,
        StopCategory category,
        Integer rating,
        int position) {
}
