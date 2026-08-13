package com.travelmemory.dna.dto;

public record TravelPersonalityResponse(
        String type,
        String description,
        boolean revealed,
        int completedTrips,
        int completedTripsRequired,
        TravelDnaScoresResponse scores) {
}
