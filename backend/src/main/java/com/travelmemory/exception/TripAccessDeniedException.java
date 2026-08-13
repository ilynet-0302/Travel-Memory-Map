package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class TripAccessDeniedException extends DomainException {
    public TripAccessDeniedException() {
        super("TRIP_ACCESS_DENIED", "You do not have permission to perform this action.", HttpStatus.FORBIDDEN);
    }
}
