package com.travelmemory.rating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpsertTripRatingRequest(
        @NotNull @Min(1) @Max(10) Integer score) {
}
