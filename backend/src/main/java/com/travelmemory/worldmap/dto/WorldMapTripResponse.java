package com.travelmemory.worldmap.dto;

import com.travelmemory.trip.entity.TripStatus;

import java.time.LocalDate;
import java.util.UUID;

public record WorldMapTripResponse(
        UUID id,
        String title,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        TripStatus status) {
}
