package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class AlreadyTripMemberException extends DomainException {
    public AlreadyTripMemberException() {
        super("ALREADY_TRIP_MEMBER", "You are already a member of this trip.", HttpStatus.CONFLICT);
    }
}
