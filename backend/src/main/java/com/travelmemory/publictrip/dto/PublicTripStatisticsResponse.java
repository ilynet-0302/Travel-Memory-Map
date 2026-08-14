package com.travelmemory.publictrip.dto;

import java.math.BigDecimal;

public record PublicTripStatisticsResponse(
        long travelDays,
        long placeCount,
        long selectedPhotoCount,
        BigDecimal totalDistanceKm) {
}
