package com.travelmemory.location.dto;

import java.math.BigDecimal;

public record GoogleMapsPlaceResponse(
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        String resolvedUrl) {
}
