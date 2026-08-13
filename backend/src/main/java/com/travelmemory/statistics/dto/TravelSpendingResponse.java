package com.travelmemory.statistics.dto;

import java.math.BigDecimal;

public record TravelSpendingResponse(
        String currency,
        BigDecimal totalSpent,
        BigDecimal averageCostPerTrip,
        BigDecimal averageCostPerDay,
        SpendingTripResponse mostExpensiveTrip,
        SpendingTripResponse cheapestTrip) {
}
