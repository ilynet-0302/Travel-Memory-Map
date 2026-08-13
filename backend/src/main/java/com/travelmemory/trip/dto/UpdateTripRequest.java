package com.travelmemory.trip.dto;

import com.travelmemory.trip.entity.TripVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTripRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 100) String country,
        @Size(min = 2, max = 2) String countryCode,
        @NotBlank @Size(max = 100) String city,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull TripVisibility visibility) {
}
