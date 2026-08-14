package com.travelmemory.photo.dto;

import jakarta.validation.constraints.NotNull;

public record UpdatePhotoPublicVisibilityRequest(
        @NotNull Boolean publicVisible) {
}
