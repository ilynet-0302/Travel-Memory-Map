package com.travelmemory.statistics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SpendingTripResponse(
        UUID tripId,
        String title,
        BigDecimal amount) {
}
