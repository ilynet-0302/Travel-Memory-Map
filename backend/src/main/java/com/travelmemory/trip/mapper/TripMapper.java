package com.travelmemory.trip.mapper;

import com.travelmemory.trip.dto.TripDetailsResponse;
import com.travelmemory.trip.dto.TripStopResponse;
import com.travelmemory.trip.dto.TripSummaryResponse;
import com.travelmemory.membership.entity.TripRole;
import com.travelmemory.trip.entity.Trip;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TripMapper {

    public TripSummaryResponse toSummary(
            Trip trip, TripRole currentUserRole, long memberCount, long stopCount, long photoCount) {
        return new TripSummaryResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getDescription(),
                trip.getCountry(),
                trip.getCountryCode(),
                trip.getCity(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus(),
                trip.getVisibility(),
                trip.getCoverImageUrl(),
                currentUserRole,
                memberCount,
                stopCount,
                photoCount);
    }

    public TripDetailsResponse toDetails(
            Trip trip, TripRole currentUserRole, long memberCount, long photoCount, List<TripStopResponse> stops) {
        return new TripDetailsResponse(
                trip.getId(),
                trip.getOwner().getId(),
                trip.getTitle(),
                trip.getDescription(),
                trip.getCountry(),
                trip.getCountryCode(),
                trip.getCity(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus(),
                trip.getVisibility(),
                trip.getCoverImageUrl(),
                currentUserRole,
                memberCount,
                photoCount,
                stops);
    }
}
