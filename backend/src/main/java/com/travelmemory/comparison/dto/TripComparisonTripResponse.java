package com.travelmemory.comparison.dto;

import com.travelmemory.rating.dto.TripRatingBreakdownResponse;
import com.travelmemory.trip.entity.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TripComparisonTripResponse(
        UUID id,
        String title,
        String country,
        String countryCode,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        TripStatus status,
        String coverImageUrl,
        long travelDays,
        long stopCount,
        long photoCount,
        long ratingCount,
        BigDecimal averageScore,
        BigDecimal wouldReturnYesPercent,
        TripRatingBreakdownResponse ratings,
        List<TripComparisonSpendingResponse> spending) {
}
