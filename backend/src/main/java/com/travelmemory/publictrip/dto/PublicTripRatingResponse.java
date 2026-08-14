package com.travelmemory.publictrip.dto;

import com.travelmemory.rating.dto.ReturnIntentSummaryResponse;
import com.travelmemory.rating.dto.TripRatingBreakdownResponse;

import java.math.BigDecimal;

public record PublicTripRatingResponse(
        BigDecimal averageScore,
        long ratingCount,
        TripRatingBreakdownResponse averages,
        ReturnIntentSummaryResponse returnIntent) {
}
