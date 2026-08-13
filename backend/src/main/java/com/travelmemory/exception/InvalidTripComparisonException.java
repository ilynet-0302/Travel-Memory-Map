package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class InvalidTripComparisonException extends DomainException {

    public InvalidTripComparisonException(String message) {
        super("INVALID_TRIP_COMPARISON", message, HttpStatus.BAD_REQUEST);
    }
}
