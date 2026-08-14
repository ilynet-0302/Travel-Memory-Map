package com.travelmemory.memory.dto;

import java.time.LocalDate;
import java.util.List;

public record OnThisDayResponse(
        LocalDate date,
        List<OnThisDayMemoryResponse> memories) {
}
