package com.travelmemory.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleMapsImportRequest(
        @NotBlank @Size(max = 2048) String url) {
}
