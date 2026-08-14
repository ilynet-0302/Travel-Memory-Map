package com.travelmemory.trip.dto;

import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.trip.entity.TripStatus;
import com.travelmemory.trip.entity.TripVisibility;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TripDetailsResponse(
        UUID id,
        UUID ownerId,
        String title,
        String description,
        String country,
        String countryCode,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        TripStatus status,
        TripVisibility visibility,
        String coverImageUrl,
        String publicSlug,
        TripRole currentUserRole,
        long memberCount,
        long photoCount,
        List<TripStopResponse> stops) {
}
