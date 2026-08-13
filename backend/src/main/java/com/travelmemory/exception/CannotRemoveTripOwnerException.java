package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class CannotRemoveTripOwnerException extends DomainException {
    public CannotRemoveTripOwnerException() {
        super("CANNOT_REMOVE_TRIP_OWNER", "The trip owner cannot be removed.", HttpStatus.CONFLICT);
    }
}
