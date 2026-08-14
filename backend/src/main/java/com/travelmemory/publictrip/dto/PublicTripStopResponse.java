package com.travelmemory.publictrip.dto;

import com.travelmemory.trip.entity.StopCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicTripStopResponse(
        UUID id,
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
