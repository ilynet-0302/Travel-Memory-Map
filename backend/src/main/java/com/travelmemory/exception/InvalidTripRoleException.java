package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class InvalidTripRoleException extends DomainException {
    public InvalidTripRoleException(String message) {
        super("INVALID_TRIP_ROLE", message, HttpStatus.BAD_REQUEST);
    }
}
