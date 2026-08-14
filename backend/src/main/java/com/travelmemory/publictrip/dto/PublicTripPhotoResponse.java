package com.travelmemory.publictrip.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicTripPhotoResponse(
        UUID id,
        UUID tripStopId,
        String signedUrl,
        String caption,
        OffsetDateTime takenAt) {
}
