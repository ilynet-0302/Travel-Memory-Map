package com.travelmemory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiErrorResponse> handleDomainException(DomainException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ApiErrorResponse(
                exception.getCode(),
                exception.getMessage(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "VALIDATION_FAILED",
                "The request contains invalid values.",
                OffsetDateTime.now(ZoneOffset.UTC),
                fieldErrors));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
                "INVALID_STATE",
                exception.getMessage(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new ApiErrorResponse(
                "PHOTO_TOO_LARGE",
                "Photos cannot be larger than 10 MB.",
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()));
    }
}
