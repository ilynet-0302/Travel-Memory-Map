package com.travelmemory.trip.dto;

import com.travelmemory.trip.entity.StopCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UpdateTripStopRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @NotNull OffsetDateTime arrivalTime,
        OffsetDateTime departureTime,
        @NotNull StopCategory category,
        @Min(1) @Max(10) Integer rating,
        @PositiveOrZero int position) {
}
