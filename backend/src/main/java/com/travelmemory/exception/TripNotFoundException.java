package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class TripNotFoundException extends DomainException {
    public TripNotFoundException(UUID tripId) {
        super("TRIP_NOT_FOUND", "Trip %s was not found.".formatted(tripId), HttpStatus.NOT_FOUND);
    }
}
