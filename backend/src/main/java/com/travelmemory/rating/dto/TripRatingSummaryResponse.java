package com.travelmemory.rating.dto;

import java.math.BigDecimal;

public record TripRatingSummaryResponse(
        BigDecimal averageScore,
        long ratingCount,
        Integer currentUserScore,
        boolean canRate) {
}
