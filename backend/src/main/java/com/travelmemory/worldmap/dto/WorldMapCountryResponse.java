package com.travelmemory.worldmap.dto;

import java.util.List;

public record WorldMapCountryResponse(
        String countryCode,
        String name,
        WorldMapCountryStatus status,
        long tripCount,
        long visitedTripCount,
        long plannedTripCount,
        long cityCount,
        long placeCount,
        long photoCount,
        List<WorldMapSpendingResponse> spending,
        List<WorldMapTripResponse> trips) {
}
