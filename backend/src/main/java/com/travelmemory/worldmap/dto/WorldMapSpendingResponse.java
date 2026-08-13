package com.travelmemory.worldmap.dto;

import java.math.BigDecimal;

public record WorldMapSpendingResponse(
        String currency,
        BigDecimal totalSpent) {
}
