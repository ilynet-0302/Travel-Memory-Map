package com.travelmemory.trip.dto;

import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.trip.entity.TripStatus;
import com.travelmemory.trip.entity.TripVisibility;

import java.time.LocalDate;
import java.util.UUID;

public record TripSummaryResponse(
        UUID id,
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
        TripRole currentUserRole,
        long memberCount,
        long stopCount,
        long photoCount) {
}
