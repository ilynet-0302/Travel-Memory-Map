package com.travelmemory.memory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MemoryPhotoResponse(
        UUID id,
        UUID tripId,
        String tripTitle,
        String country,
        String countryCode,
        String city,
        LocalDate tripStartDate,
        LocalDate tripEndDate,
        UUID tripStopId,
        String tripStopName,
        UUID uploadedByUserId,
        String uploadedByDisplayName,
        String signedUrl,
        String originalFileName,
        String contentType,
        long fileSize,
        OffsetDateTime takenAt,
        BigDecimal latitude,
        BigDecimal longitude,
        String caption,
        boolean publicVisible,
        OffsetDateTime createdAt) {
}
