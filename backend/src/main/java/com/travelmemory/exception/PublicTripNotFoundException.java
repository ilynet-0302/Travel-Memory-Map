package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class PublicTripNotFoundException extends DomainException {
    public PublicTripNotFoundException() {
        super("PUBLIC_TRIP_NOT_FOUND", "This public trip does not exist.", HttpStatus.NOT_FOUND);
    }
}
