package com.travelmemory.statistics.dto;

import java.util.List;

public record TravelStatisticsResponse(
        int countriesVisited,
        int citiesVisited,
        int trips,
        int completedTrips,
        long placesVisited,
        long photosUploaded,
        long travelDays,
        List<TravelSpendingResponse> spending,
        LocationStatisticResponse favouriteCountry,
        LocationStatisticResponse favouriteCity,
        LocationStatisticResponse mostVisitedCountry,
        TripStatisticResponse longestTrip,
        TripStatisticResponse shortestTrip) {
}
