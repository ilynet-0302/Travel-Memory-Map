package com.travelmemory.trip.dto;

import com.travelmemory.trip.entity.StopCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.UUID;

public record TripStopResponse(
        UUID id,
        UUID createdByUserId,
        String name,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        OffsetDateTime arrivalTime,
        LocalDateTime arrivalLocalDateTime,
        OffsetDateTime departureTime,
        LocalDateTime departureLocalDateTime,
        StopCategory category,
        Integer rating,
        int position) {
}
