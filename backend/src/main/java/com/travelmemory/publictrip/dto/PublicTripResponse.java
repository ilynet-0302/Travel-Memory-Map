package com.travelmemory.publictrip.dto;

import com.travelmemory.replay.dto.ReplayRouteSegmentResponse;
import com.travelmemory.replay.dto.ReplayRouteSource;
import com.travelmemory.trip.entity.TripStatus;

import java.time.LocalDate;
import java.util.List;

public record PublicTripResponse(
        String publicSlug,
        String title,
        String description,
        String country,
        String countryCode,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        TripStatus status,
        String coverImageUrl,
        PublicTripStatisticsResponse statistics,
        ReplayRouteSource routeSource,
        List<ReplayRouteSegmentResponse> routeSegments,
        List<PublicTripStopResponse> stops,
        List<PublicTripPhotoResponse> photos,
        PublicTripRatingResponse rating) {
}
