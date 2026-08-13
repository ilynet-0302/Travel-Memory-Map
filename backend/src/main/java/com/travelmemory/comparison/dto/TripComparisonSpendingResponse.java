package com.travelmemory.comparison.dto;

import java.math.BigDecimal;

public record TripComparisonSpendingResponse(
        String currency,
        BigDecimal total,
        BigDecimal costPerDay) {
}
