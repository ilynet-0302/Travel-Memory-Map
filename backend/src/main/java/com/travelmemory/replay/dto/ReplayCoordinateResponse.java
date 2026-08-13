package com.travelmemory.replay.dto;

import java.math.BigDecimal;

public record ReplayCoordinateResponse(
        BigDecimal longitude,
        BigDecimal latitude) {
}
