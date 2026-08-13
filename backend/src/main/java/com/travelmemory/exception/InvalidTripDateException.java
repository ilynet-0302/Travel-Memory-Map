package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class InvalidTripDateException extends DomainException {
    public InvalidTripDateException(String message) {
        super("INVALID_TRIP_DATE", message, HttpStatus.BAD_REQUEST);
    }
}
