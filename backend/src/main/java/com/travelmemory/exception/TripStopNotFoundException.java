package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class TripStopNotFoundException extends DomainException {
    public TripStopNotFoundException(UUID stopId) {
        super("TRIP_STOP_NOT_FOUND", "Trip stop %s was not found.".formatted(stopId), HttpStatus.NOT_FOUND);
    }
}
