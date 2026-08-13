package com.travelmemory.statistics.dto;

public record TravelStatisticsResponse(
        int countriesVisited,
        int citiesVisited,
        int trips,
        int completedTrips,
        long placesVisited,
        long photosUploaded,
        long travelDays) {
}
