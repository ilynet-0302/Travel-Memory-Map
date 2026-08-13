package com.travelmemory.comparison.dto;

public record TripComparisonResponse(
        TripComparisonTripResponse left,
        TripComparisonTripResponse right) {
}
