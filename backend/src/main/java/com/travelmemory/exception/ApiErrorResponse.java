package com.travelmemory.exception;

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiErrorResponse(
        String code,
        String message,
        OffsetDateTime timestamp,
        Map<String, String> fieldErrors) {
}
