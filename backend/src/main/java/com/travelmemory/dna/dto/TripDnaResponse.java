package com.travelmemory.dna.dto;

import java.util.List;
import java.util.UUID;

public record TripDnaResponse(
        UUID tripId,
        TravelDnaScoresResponse scores,
        String dominantTrait,
        List<String> signals) {
}
