package com.travelmemory.statistics.dto;

import java.time.LocalDate;
import java.util.UUID;

public record TripStatisticResponse(
        UUID tripId,
        String title,
        String country,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        long travelDays) {
}
