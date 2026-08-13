package com.travelmemory.photo.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExtractedPhotoMetadata(
        OffsetDateTime takenAt,
        BigDecimal latitude,
        BigDecimal longitude) {

    public static ExtractedPhotoMetadata empty() {
        return new ExtractedPhotoMetadata(null, null, null);
    }
}
