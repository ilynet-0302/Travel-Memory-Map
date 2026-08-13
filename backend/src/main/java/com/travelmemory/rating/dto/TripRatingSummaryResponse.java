package com.travelmemory.rating.dto;

import java.math.BigDecimal;

public record TripRatingSummaryResponse(
        BigDecimal averageScore,
        long ratingCount,
        TripRatingResponse currentUserRating,
        TripRatingBreakdownResponse averages,
        ReturnIntentSummaryResponse returnIntent,
        boolean canRate) {
}
