package com.travelmemory.exception;

import org.springframework.http.HttpStatus;

public class InvalidTripStopOrderException extends DomainException {

    public InvalidTripStopOrderException() {
        super(
                "INVALID_TRIP_STOP_ORDER",
                "The stop order must contain every place in this trip exactly once.",
                HttpStatus.BAD_REQUEST);
    }
}
