package com.travelmemory.replay.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReplayPhotoResponse(
        UUID id,
        String signedUrl,
        String caption,
        OffsetDateTime takenAt) {
}

