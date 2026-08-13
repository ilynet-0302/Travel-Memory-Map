package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class OwnerCannotLeaveTripException extends DomainException {
    public OwnerCannotLeaveTripException() {
        super("OWNER_CANNOT_LEAVE_TRIP", "The trip owner cannot leave the trip.", HttpStatus.CONFLICT);
    }
}
