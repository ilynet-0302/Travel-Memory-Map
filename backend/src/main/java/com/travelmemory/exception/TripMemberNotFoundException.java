package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class TripMemberNotFoundException extends DomainException {
    public TripMemberNotFoundException() {
        super("TRIP_MEMBER_NOT_FOUND", "This trip member does not exist.", HttpStatus.NOT_FOUND);
    }
}
