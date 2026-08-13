package com.travelmemory.rating.dto;

import java.math.BigDecimal;

public record TripRatingBreakdownResponse(
        BigDecimal food,
        BigDecimal nightlife,
        BigDecimal culture,
        BigDecimal nature,
        BigDecimal walkability,
        BigDecimal valueForMoney,
        BigDecimal crowds,
        BigDecimal relaxation) {
}
