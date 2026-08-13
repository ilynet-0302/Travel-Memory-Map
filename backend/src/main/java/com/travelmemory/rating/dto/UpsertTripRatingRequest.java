package com.travelmemory.rating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.travelmemory.rating.entity.WouldReturn;

public record UpsertTripRatingRequest(
        @NotNull @Min(1) @Max(10) Integer food,
        @NotNull @Min(1) @Max(10) Integer nightlife,
        @NotNull @Min(1) @Max(10) Integer culture,
        @NotNull @Min(1) @Max(10) Integer nature,
        @NotNull @Min(1) @Max(10) Integer walkability,
        @NotNull @Min(1) @Max(10) Integer valueForMoney,
        @NotNull @Min(1) @Max(10) Integer crowds,
        @NotNull @Min(1) @Max(10) Integer relaxation,
        @NotNull WouldReturn wouldReturn) {
}
