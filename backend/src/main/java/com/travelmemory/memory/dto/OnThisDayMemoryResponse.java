package com.travelmemory.memory.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OnThisDayMemoryResponse(
        UUID tripId,
        String tripTitle,
        String country,
        String countryCode,
        String city,
        LocalDate memoryDate,
        int yearsAgo,
        long placeCount,
        long photoCount,
        List<OnThisDaySpendingResponse> spending,
        String heroPhotoUrl,
        String heroPhotoCaption,
        List<String> placeNames) {
}
