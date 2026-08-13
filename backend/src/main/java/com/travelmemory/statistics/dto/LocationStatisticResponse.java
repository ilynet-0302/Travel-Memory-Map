package com.travelmemory.statistics.dto;

import java.math.BigDecimal;

public record LocationStatisticResponse(
        String name,
        long tripCount,
        BigDecimal averageRating) {
}
