package com.travelmemory.memory.dto;

import java.math.BigDecimal;

public record OnThisDaySpendingResponse(
        String currency,
        BigDecimal totalSpent) {
}
