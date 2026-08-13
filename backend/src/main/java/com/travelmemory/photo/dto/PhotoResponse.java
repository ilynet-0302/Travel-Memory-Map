package com.travelmemory.photo.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PhotoResponse(
        UUID id,
        UUID tripId,
        UUID tripStopId,
        UUID uploadedByUserId,
        String uploadedByDisplayName,
        String storagePath,
        String signedUrl,
        String originalFileName,
        String contentType,
        long fileSize,
        OffsetDateTime takenAt,
        BigDecimal latitude,
        BigDecimal longitude,
        String caption,
        OffsetDateTime createdAt) {
}
